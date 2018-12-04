package imgui.imgui

import imgui.ImGui.currentWindow
import imgui.ImGui.isItemVisible
import imgui.ImGui.setScrollHereY
import imgui.g
import imgui.internal.Rect
import imgui.navUpdateAnyRequestFlag


/** Focus, Activation
 *  (Prefer using "setItemDefaultFocus()" over "if (isWindowAppearing()) setScrollHereY()" when applicable,
 *  to make your code more forward compatible when navigation branch is merged) */
interface imgui_focusActivation {

    // (Prefer using "SetItemDefaultFocus()" over "if (IsWindowAppearing()) SetScrollHereY()" when applicable to signify "this is the default item")

    /** make last item the default focused item of a window. */
    fun setItemDefaultFocus() {
        val window = g.currentWindow!!
        if (!window.appearing) return
        val nav = g.navWindow!!
        if (nav === window.rootWindowForNav && (g.navInitRequest || g.navInitResultId != 0) && g.navLayer == nav.dc.navLayerCurrent) {
            g.navInitRequest = false
            g.navInitResultId = nav.dc.lastItemId
            g.navInitResultRectRel = Rect(nav.dc.lastItemRect.min - nav.pos, nav.dc.lastItemRect.max - nav.pos)
            navUpdateAnyRequestFlag()
            if (!isItemVisible) setScrollHereY()
        }
    }

    /** focus keyboard on the next widget. Use positive 'offset' to access sub components of a multiple component widget.
     *  Use -1 to access previous widget.   */
    fun setKeyboardFocusHere(offset: Int = 0) = with(currentWindow) {
        assert(offset >= -1) { "-1 is allowed but not below" }
        focusIdxAllRequestNext = focusIdxAllCounter + 1 + offset
        focusIdxTabRequestNext = Int.MAX_VALUE
    }
}