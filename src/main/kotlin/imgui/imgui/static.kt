package imgui.imgui

import gli_.hasnt
import glm_.glm
import glm_.vec2.Vec2
import imgui.*
import imgui.ImGui.calcTextSize
import imgui.ImGui.isMouseHoveringRect
import imgui.ImGui.navInitWindow
import imgui.ImGui.style
import imgui.internal.*
import kotlin.math.abs
import imgui.Context as g
import imgui.WindowFlags as Wf

fun navScoreItemGetQuadrant(dx: Float, dy: Float) = when {
    abs(dx) > abs(dy) -> when {
        dx > 0f -> Dir.Right
        else -> Dir.Left
    }
    else -> when {
        dy > 0f -> Dir.Down
        else -> Dir.Up
    }
}

fun navScoreItemDistInterval(a0: Float, a1: Float, b0: Float, b1: Float) = when {
    a1 < b0 -> a1 - b0
    b1 < a0 -> a0 - b1
    else -> 0f
}

/** Scoring function for directional navigation. Based on https://gist.github.com/rygorous/6981057  */
fun navScoreItem(result: NavMoveResult, cand: Rect): Boolean {

    val window = g.currentWindow!!
    if (g.navLayer != window.dc.navLayerCurrent) return false

    // Current modified source rect (NB: we've applied max.x = min.x in navUpdate() to inhibit the effect of having varied item width)
    val curr = Rect(g.navScoringRectScreen)
    g.navScoringCount++

    // We perform scoring on items bounding box clipped by their parent window on the other axis (clipping on our movement axis would give us equal scores for all clipped items)
    if (g.navMoveDir == Dir.Left || g.navMoveDir == Dir.Right) {
        cand.min.y = glm.clamp(cand.min.y, window.clipRect.min.y, window.clipRect.max.y)
        cand.max.y = glm.clamp(cand.max.y, window.clipRect.min.y, window.clipRect.max.y)
    } else {
        cand.min.x = glm.clamp(cand.min.x, window.clipRect.min.x, window.clipRect.max.x)
        cand.max.x = glm.clamp(cand.max.x, window.clipRect.min.x, window.clipRect.max.x)
    }

    // Compute distance between boxes
    // FIXME-NAV: Introducing biases for vertical navigation, needs to be removed.
    var dbX = navScoreItemDistInterval(cand.min.x, cand.max.x, curr.min.x, curr.max.x)
    // Scale down on Y to keep using box-distance for vertically touching items
    val dbY = navScoreItemDistInterval(lerp(cand.min.y, cand.max.y, 0.2f), lerp(cand.min.y, cand.max.y, 0.8f),
            lerp(curr.min.y, curr.max.y, 0.2f), lerp(curr.min.y, curr.max.y, 0.8f))
    if (dbY != 0f && dbX != 0f)
        dbX = dbX / 1000f + if (dbX > 0f) 1f else -1f
    val distBox = abs(dbX) + abs(dbY)

    // Compute distance between centers (this is off by a factor of 2, but we only compare center distances with each other so it doesn't matter)
    val dcX = (cand.min.x + cand.max.x) - (curr.min.x + curr.max.x)
    val dcY = (cand.min.y + cand.max.y) - (curr.min.y + curr.max.y)
    val distCenter = abs(dcX) + abs(dcY) // L1 metric (need this for our connectedness guarantee)

    // Determine which quadrant of 'curr' our candidate item 'cand' lies in based on distance
    val quadrant: Dir
    var dax = 0f
    var day = 0f
    var distAxial = 0f
    if (dbX != 0f || dbY != 0f) {
        // For non-overlapping boxes, use distance between boxes
        dax = dbX
        day = dbY
        distAxial = distBox
        quadrant = navScoreItemGetQuadrant(dbX, dbY)
    } else if (dcX != 0f || dcY != 0f) {
        // For overlapping boxes with different centers, use distance between centers
        dax = dcX
        day = dcY
        distAxial = distCenter
        quadrant = navScoreItemGetQuadrant(dcX, dcY)
    }
    /* Degenerate case: two overlapping buttons with same center, break ties arbitrarily (note that lastItemId here is
        really the _previous_ item order, but it doesn't matter)     */
    else
        quadrant = if (window.dc.lastItemId < g.navId) Dir.Left else Dir.Right

    if (IMGUI_DEBUG_NAV_SCORING)
        if (isMouseHoveringRect(cand.min, cand.max)) {
            val buf = "dbox (%.2f,%.2f->%.4f)\ndcen (%.2f,%.2f->%.4f)\nd (%.2f,%.2f->%.4f)\nnav WENS${g.navMoveDir}, quadrant WENS$quadrant"
                    .format(style.locale, dbX, dbY, distBox, dcX, dcY, distCenter, dax, day, distAxial)
            with(g.overlayDrawList) {
                addRect(curr.min, curr.max, COL32(255, 200, 0, 100))
                addRect(cand.min, cand.max, COL32(255, 255, 0, 200))
                addRectFilled(cand.max - Vec2(4), cand.max + calcTextSize(buf) + Vec2(4), COL32(40, 0, 0, 150))
                addText(IO.fontDefault, 13f, cand.max, 0.inv(), buf.toCharArray())
            }
        } else if (IO.keyCtrl) { // Hold to preview score in matching quadrant. Press C to rotate.
            if (Key.C.isPressed) {
                g.navMoveDirLast = Dir.of((g.navMoveDirLast.i + 1) and 3)
                IO.keysDownDuration[IO.keyMap[Key.C]] = 0.01f
            }
            if (quadrant == g.navMoveDir) {
                val buf = "%.0f/%.0f".format(style.locale, distBox, distCenter).toCharArray()
                g.overlayDrawList.addRectFilled(cand.min, cand.max, COL32(255, 0, 0, 200))
                g.overlayDrawList.addText(IO.fontDefault, 13f, cand.min, COL32(255, 255, 255, 255), buf)
            }
        }

    // Is it in the quadrant we're interesting in moving to?
    var newBest = false
    if (quadrant == g.navMoveDir) {
        // Does it beat the current best candidate?
        if (distBox < result.distBox) {
            result.distBox = distBox
            result.distCenter = distCenter
            return true
        }
        if (distBox == result.distBox) {
            // Try using distance between center points to break ties
            if (distCenter < result.distCenter) {
                result.distCenter = distCenter
                newBest = true
            } else if (distCenter == result.distCenter) {
                /*  Still tied! we need to be extra-careful to make sure everything gets linked properly.
                    We consistently break ties by symbolically moving "later" items (with higher index) to the
                    right/downwards by an infinitesimal amount since we the current "best" button already
                    (so it must have a lower index), this is fairly easy.
                    This rule ensures that all buttons with dx == dy == 0 will end up being linked in order
                    of appearance along the x axis. */
                val db = if (g.navMoveDir == Dir.Up || g.navMoveDir == Dir.Down) dbY else dbX
                if (db < 0f) // moving bj to the right/down decreases distance
                    newBest = true
            }
        }
    }

    /*  Axial check: if 'curr' has no link at all in some direction and 'cand' lies roughly in that direction,
        add a tentative link. This will only be kept if no "real" matches are found, so it only augments the graph
        produced by the above method using extra links. (important, since it doesn't guarantee strong connectedness)
        This is just to avoid buttons having no links in a particular direction when there's a suitable neighbor.
        You get good graphs without this too.
        2017/09/29: FIXME: This now currently only enabled inside menu bars, ideally we'd disable it everywhere.
        Menus in particular need to catch failure. For general navigation it feels awkward.
        Disabling it may however lead to disconnected graphs when nodes are very spaced out on different axis.
        Perhaps consider offering this as an option?    */
    if (result.distBox == Float.MAX_VALUE && distAxial < result.distAxial)  // Check axial match
        if (g.navLayer == 1 && g.navWindow!!.flags hasnt Wf.ChildMenu)
            if ((g.navMoveDir == Dir.Left && dax < 0f) || (g.navMoveDir == Dir.Right && dax > 0f) ||
                    (g.navMoveDir == Dir.Up && day < 0f) || (g.navMoveDir == Dir.Down && day > 0f)) {
                result.distAxial = distAxial
                newBest = true
            }

    return newBest
}

fun navSaveLastChildNavWindow(childWindow: Window?) {
    var parentWindow = childWindow
    while (parentWindow != null && parentWindow.flags has Wf.ChildWindow && parentWindow.flags hasnt (Wf.Popup or Wf.ChildMenu))
        parentWindow = parentWindow.parentWindow
    parentWindow?.let { if (it !== childWindow) it.navLastChildNavWindow = childWindow }
}

/** Call when we are expected to land on Layer 0 after FocusWindow()    */
fun navRestoreLastChildNavWindow(window: Window) = window.navLastChildNavWindow ?: window

fun navRestoreLayer(layer: Int) {

    g.navLayer = layer
    if (layer == 0)
        g.navWindow = navRestoreLastChildNavWindow(g.navWindow!!)
    if (layer == 0 && g.navWindow!!.navLastIds[0] != 0)
        setNavIdAndMoveMouse(g.navWindow!!.navLastIds[0], layer, g.navWindow!!.navRectRel[0])
    else
        navInitWindow(g.navWindow!!, true)
}

fun navUpdateAnyRequestFlag() {
    g.navAnyRequest = g.navMoveRequest || g.navInitRequest || IMGUI_DEBUG_NAV_SCORING
}

fun navMoveRequestButNoResultYet() = g.navMoveRequest && g.navMoveResultLocal.id == 0 && g.navMoveResultOther.id == 0

fun navMoveRequestCancel() {
    g.navMoveRequest = false
    navUpdateAnyRequestFlag()
}


fun setCurrentWindow(window: Window?) {
    g.currentWindow = window
    if (window != null)
        g.fontSize = window.calcFontSize()
    g.drawListSharedData.fontSize = g.fontSize
}

fun setNavId(id: Int, navLayer: Int) {
    assert(navLayer == 0 || navLayer == 1)
    g.navId = id
    g.navWindow!!.navLastIds[navLayer] = id
}

fun setNavIdAndMoveMouse(id: Int, navLayer: Int, rectRel: Rect) {
    setNavId(id, navLayer)
    g.navWindow!!.navRectRel[navLayer] put rectRel
    g.navMousePosDirty = true
    g.navDisableHighlight = false
    g.navDisableMouseHover = true
}