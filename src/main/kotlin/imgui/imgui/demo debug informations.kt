package imgui.imgui

import glm_.BYTES
import glm_.f
import glm_.vec2.Vec2
import glm_.vec4.Vec4
import imgui.*
import imgui.Context.overlayDrawList
import imgui.Context.style
import imgui.ImGui._begin
import imgui.ImGui.beginCombo
import imgui.ImGui.beginTooltip
import imgui.ImGui.bulletText
import imgui.ImGui.checkbox
import imgui.ImGui.colorButton
import imgui.ImGui.combo
import imgui.ImGui.end
import imgui.ImGui.endCombo
import imgui.ImGui.endTooltip
import imgui.ImGui.font
import imgui.ImGui.getStyleColorVec4
import imgui.ImGui.inputFloat
import imgui.ImGui.isItemHovered
import imgui.ImGui.menuItem
import imgui.ImGui.popTextWrapPos
import imgui.ImGui.pushTextWrapPos
import imgui.ImGui.sameLine
import imgui.ImGui.selectable
import imgui.ImGui.separator
import imgui.ImGui.sliderFloat
import imgui.ImGui.styleColorsClassic
import imgui.ImGui.styleColorsDark
import imgui.ImGui.styleColorsLight
import imgui.ImGui.text
import imgui.ImGui.textColored
import imgui.ImGui.textDisabled
import imgui.ImGui.textUnformatted
import imgui.ImGui.treeNode
import imgui.ImGui.treePop
import imgui.ImGui.version
import imgui.ImGui.windowDrawList
import imgui.functionalProgramming.menu
import imgui.functionalProgramming.withChild
import imgui.functionalProgramming.withIndent
import imgui.functionalProgramming.withStyleVar
import imgui.imgui.demo.ExampleApp
import imgui.internal.DrawListFlags
import imgui.internal.Rect
import imgui.internal.Window
import java.util.*
import kotlin.reflect.KMutableProperty0
import imgui.ColorEditFlags as Cef
import imgui.Context as g
import imgui.InputTextFlags as Itf
import imgui.SelectableFlags as Sf
import imgui.TreeNodeFlags as Tnf
import imgui.WindowFlags as Wf

/**
 *  Message to the person tempted to delete this file when integrating ImGui into their code base:
 *  Do NOT remove this file from your project! It is useful reference code that you and other users will want to refer to.
 *  Don't do it! Do NOT remove this file from your project! It is useful reference code that you and other users will want to refer to.
 *  Everything in this file will be stripped out by the linker if you don't call ImGui::ShowDemoWindow().
 *  During development, you can call ImGui::ShowDemoWindow() in your code to learn about various features of ImGui.
 *  Removing this file from your project is hindering your access to documentation, likely leading you to poorer usage of the library.
 *  During development, you can call ImGui::ShowDemoWindow() in your code to learn about various features of ImGui. Have it wired in a debug menu!
 *  Removing this file from your project is hindering access to documentation for everyone in your team, likely leading you to poorer usage of the library.
 *
 *  Note that you can #define IMGUI_DISABLE_DEMO_WINDOWS in imconfig.h for the same effect.
 *  If you want to link core ImGui in your final builds but not those demo windows, #define IMGUI_DISABLE_DEMO_WINDOWS in imconfig.h and those functions will be empty.
 *  In other situation, when you have ImGui available you probably want this to be available for reference and execution.
 *
 *  Thank you,
 *  -Your beloved friend, imgui_demo.cpp (that you won't delete)
 */
interface imgui_demoDebugInformations {
    /** Create demo/test window.
     *  Demonstrate most ImGui features (big function!)
     *  Call this to learn about the library! try to make it always available in your application!   */
    fun showDemoWindow(open: BooleanArray) {
        showWindow = open[0]
        showDemoWindow(Companion::showWindow)
        open[0] = showWindow
    }

    fun showDemoWindow(open: KMutableProperty0<Boolean>) = ExampleApp(open)

    /** Create metrics window. display ImGui internals: draw commands (with individual draw calls and vertices), window list,
     *  basic internal state, etc.    */
    fun showMetricsWindow(open: KMutableProperty0<Boolean>) {

        if (_begin("ImGui Metrics", open)) {
            text("ImGui $version")
            text("Application average %.3f ms/frame (%.1f FPS)", 1000f / IO.framerate, IO.framerate)
            text("%d vertices, %d indices (%d triangles)", IO.metricsRenderVertices, IO.metricsRenderIndices, IO.metricsRenderIndices / 3)
            text("%d allocations", IO.metricsAllocs)
            checkbox("Show clipping rectangles when hovering an ImDrawCmd", Companion::showClipRects)
            separator()

            Funcs0.nodeWindows(g.windows, "Windows")
            if (treeNode("DrawList", "Active DrawLists (${g.renderDrawLists[0].size})")) {
                g.renderDrawLists.forEach { layer -> layer.forEach { Funcs0.nodeDrawList(it, "DrawList") } }
                for (i in g.renderDrawLists[0])
                    Funcs0.nodeDrawList(i, "DrawList")
                treePop()
            }
            if (treeNode("Popups", "Open Popups Stack (${g.openPopupStack.size})")) {
                for (popup in g.openPopupStack) {
                    val window = popup.window
                    val childWindow = if (window != null && window.flags has Wf.ChildWindow) " ChildWindow" else ""
                    val childMenu = if (window != null && window.flags has Wf.ChildMenu) " ChildMenu" else ""
                    bulletText("PopupID: %08x, Window: '${window?.name}'$childWindow$childMenu", popup.popupId)
                }
                treePop()
            }
            if (treeNode("Basic state")) {
                text("HoveredWindow: '${g.hoveredWindow?.name}'")
                text("HoveredRootWindow: '${g.hoveredWindow?.name}'")
                /*  Data is "in-flight" so depending on when the Metrics window is called we may see current frame
                    information or not                 */
                text("HoveredId: 0x%08X/0x%08X (%.2f sec)", g.hoveredId, g.hoveredIdPreviousFrame, g.hoveredIdTimer)
                text("ActiveId: 0x%08X/0x%08X (%.2f sec)", g.activeId, g.activeIdPreviousFrame, g.activeIdTimer)
                text("ActiveIdWindow: '${g.activeIdWindow?.name}'")
                text("NavWindow: '${g.navWindow?.name}'")
                text("DragDrop: ${g.dragDropActive}, SourceId = 0x%08X, Payload \"${g.dragDropPayload.dataType}\" " +
                        "(${g.dragDropPayload.dataSize} bytes)", g.dragDropPayload.sourceId)
                treePop()
            }
        }
        end()
    }

    /** Demo helper function to select among default colors. See showStyleEditor() for more advanced options.
     *  Here we use the simplified combo() api that packs items into a single literal string. Useful for quick combo
     *  boxes where the choices are known locally.  */
    fun showStyleSelector(label: String) =
            if (combo(label, ::styleIdx, "Classic\u0000Dark\u0000Light\u0000")) {
                when (styleIdx) {
                    0 -> styleColorsClassic()
                    1 -> styleColorsDark()
                    2 -> styleColorsLight()
                }
                true
            } else false

    /** Demo helper function to select among loaded fonts.
     *  Here we use the regular beginCombo()/endCombo() api which is more the more flexible one. */
    fun showFontSelector(label: String) {
        val fontCurrent = font
        if (beginCombo(label, fontCurrent.debugName)) {
            for (f in IO.fonts.fonts)
                if (selectable(f.debugName, f == fontCurrent))
                    IO.fontDefault = f
            endCombo()
        }
        sameLine()
        showHelpMarker("""
            - Load additional fonts with io.Fonts->AddFontFromFileTTF().
            - The font atlas is built when calling io.Fonts->GetTexDataAsXXXX() or io.Fonts->Build().
            - Read FAQ and documentation in extra_fonts/ for more details.
            - If you need to add/remove fonts at runtime (e.g. for DPI change), do it before calling NewFrame().""")
    }


    fun showUserGuide() {
        bulletText("Double-click on title bar to collapse window.")
        bulletText("Click and drag on lower right corner to resize window\n(double-click to auto fit window to its contents).")
        bulletText("Click and drag on any empty space to move window.")
        bulletText("TAB/SHIFT+TAB to cycle through keyboard editable fields.")
        bulletText("CTRL+Click on a slider or drag box to input value as text.")
        if (IO.fontAllowUserScaling)
            bulletText("CTRL+Mouse Wheel to zoom window contents.")
        bulletText("Mouse Wheel to scroll.")
        bulletText("While editing text:\n")
        withIndent {
            bulletText("Hold SHIFT or use mouse to select text.")
            bulletText("CTRL+Left/Right to word jump.")
            bulletText("CTRL+A or double-click to select all.")
            bulletText("CTRL+X,CTRL+C,CTRL+V to use clipboard.")
            bulletText("CTRL+Z,CTRL+Y to undo/redo.")
            bulletText("ESCAPE to revert.")
            bulletText("You can apply arithmetic operators +,*,/ on numerical values.\nUse +- to subtract.")
        }
    }

    companion object {

        var showWindow = false

        fun showHelpMarker(desc: String) {
            textDisabled("(?)")
            if (isItemHovered()) {
                beginTooltip()
                pushTextWrapPos(450f)
                textUnformatted(desc)
                popTextWrapPos()
                endTooltip()
            }
        }

        var enabled = true
        var float = 0.5f
        var combo = 0
        var check = true

        fun showExampleMenuFile() {
            menuItem("(dummy menu)", "", false, false)
            menuItem("New")
            menuItem("Open", "Ctrl+O")
            menu("Open Recent") {
                menuItem("fish_hat.c")
                menuItem("fish_hat.inl")
                menuItem("fish_hat.h")
                menu("More..") {
                    menuItem("Hello")
                    menuItem("Sailor")
                    menu("Recurse..") { showExampleMenuFile() }
                }
            }
            menuItem("Save", "Ctrl+S")
            menuItem("Save As..")
            separator()
            menu("Options") {
                menuItem("Enabled", "", Companion::enabled)
                withChild("child", Vec2(0, 60), true) {
                    for (i in 0 until 10) text("Scrolling Text %d", i)
                }
                sliderFloat("Value", Companion::float, 0f, 1f)
                inputFloat("Input", Companion::float, 0.1f, 0f, 2)
                combo("Combo", Companion::combo, "Yes\u0000No\u0000Maybe\u0000\u0000")
                checkbox("Check", Companion::check)
            }
            menu("Colors") {
                withStyleVar(StyleVar.FramePadding, Vec2()) {
                    for (col in Col.values()) {
                        val name = col.name
                        colorButton(name, getStyleColorVec4(col))
                        sameLine()
                        menuItem(name)
                    }
                }
            }
            menu("Disabled", false) { assert(false) } // Disabled
            menuItem("Checked", selected = true)
            menuItem("Quit", "Alt+F4")
        }


        object Funcs0 {

            fun nodeDrawList(drawList: DrawList, label: String) {

                val nodeOpen = treeNode(drawList, "$label: '${drawList._ownerName}' ${drawList.vtxBuffer.size} vtx, " +
                        "${drawList.idxBuffer.size} indices, ${drawList.cmdBuffer.size} cmds")
                if (drawList === windowDrawList) {
                    sameLine()
                    // Can't display stats for active draw list! (we don't have the data double-buffered)
                    textColored(Vec4.fromColor(255, 100, 100), "CURRENTLY APPENDING")
                    if (nodeOpen) treePop()
                    return
                }
                if (!nodeOpen)
                    return

                val overlayDrawList = g.overlayDrawList   // Render additional visuals into the top-most draw list
                var elemOffset = 0
                for (i in drawList.cmdBuffer.indices) {
                    val cmd = drawList.cmdBuffer[i]
                    if (cmd.userCallback == null && cmd.elemCount == 0) continue
                    if (cmd.userCallback != null) {
                        TODO()
//                        ImGui::BulletText("Callback %p, user_data %p", pcmd->UserCallback, pcmd->UserCallbackData)
//                        continue
                    }
                    val idxBuffer = drawList.idxBuffer.takeIf { it.isNotEmpty() }
                    val mode = if (drawList.idxBuffer.isNotEmpty()) "indexed" else "non-indexed"
                    val cmdNodeOpen = treeNode(i, "Draw %-4d $mode vtx, tex = ${cmd.textureId}, clip_rect = (%.0f,%.0f)..(%.0f,%.0f)",
                            cmd.elemCount, cmd.clipRect.x, cmd.clipRect.y, cmd.clipRect.z, cmd.clipRect.w)
                    if (showClipRects && isItemHovered()) {
                        val clipRect = Rect(cmd.clipRect)
                        val vtxsRect = Rect()
                        for (e in elemOffset until elemOffset + cmd.elemCount)
                            vtxsRect.add(drawList.vtxBuffer[idxBuffer?.get(e) ?: e].pos)
                        clipRect.floor(); overlayDrawList.addRect(clipRect.min, clipRect.max, COL32(255, 255, 0, 255))
                        vtxsRect.floor(); overlayDrawList.addRect(vtxsRect.min, vtxsRect.max, COL32(255, 0, 255, 255))
                    }
                    if (!cmdNodeOpen) continue
                    // Display individual triangles/vertices. Hover on to get the corresponding triangle highlighted.
                    // Manually coarse clip our print out of individual vertices to save CPU, only items that may be visible.
                    val clipper = ListClipper(cmd.elemCount / 3)
                    while (clipper.step()) {
                        var vtxI = elemOffset + clipper.display.start * 3
                        for (prim in clipper.display.start until clipper.display.last) {
                            val buf = CharArray(300)
                            var bufP = 0
                            val trianglesPos = arrayListOf(Vec2(), Vec2(), Vec2())
                            for (n in 0 until 3) {
                                val v = drawList.vtxBuffer[idxBuffer?.get(vtxI) ?: vtxI]
                                trianglesPos[n] = v.pos
                                val name = if (n == 0) "vtx" else "   "
                                val string = "$name %04d { pos = (%8.2f,%8.2f), uv = (%.6f,%.6f), col = %08X }\n".format(style.locale,
                                        vtxI, v.pos.x, v.pos.y, v.uv.x, v.uv.y, v.col)
                                string.toCharArray(buf, bufP)
                                bufP += string.length
                                vtxI++
                            }
                            selectable(buf.joinToString("", limit = bufP, truncated = ""), false)
                            if (isItemHovered()) {
                                val backupFlags = overlayDrawList.flags
                                // Disable AA on triangle outlines at is more readable for very large and thin triangles.
                                overlayDrawList.flags = overlayDrawList.flags and DrawListFlags.AntiAliasedLines.i.inv()
                                overlayDrawList.addPolyline(trianglesPos, COL32(255, 255, 0, 255), true, 1f)
                                overlayDrawList.flags = backupFlags
                            }

                        }
                    }
                    treePop()
                    elemOffset += cmd.elemCount
                }
                treePop()
            }

            fun nodeWindows(windows: ArrayList<Window>, label: String) {
                if (!treeNode(label, "$label (${windows.size})")) return
                for (i in 0 until windows.size)
                    nodeWindow(windows[i], "Window")
                treePop()
            }

            fun nodeWindow(window: Window, label: String) {
                val active = if (window.active or window.wasActive) "active" else "inactive"
                if (!treeNode(window, "$label '${window.name}', $active @ 0x%X", System.identityHashCode(window)))
                    return
                nodeDrawList(window.drawList, "DrawList")
                bulletText("Pos: (%.1f,%.1f), Size: (%.1f,%.1f), SizeContents (%.1f,%.1f)", window.pos.x.f, window.pos.y.f,
                        window.size.x, window.size.y, window.sizeContents.x, window.sizeContents.y)
                if (isItemHovered())
                    overlayDrawList.addRect(Vec2(window.pos), Vec2(window.pos + window.size), COL32(255, 255, 0, 255))
                bulletText("Scroll: (%.2f/%.2f,%.2f/%.2f)", window.scroll.x, window.scrollMaxX, window.scroll.y, window.scrollMaxY)
                bulletText("Active: ${window.active}, WriteAccessed: ${window.writeAccessed}")
                if (window.rootWindow !== window) nodeWindow(window.rootWindow!!, "RootWindow")
                if (window.dc.childWindows.isNotEmpty()) nodeWindows(window.dc.childWindows, "ChildWindows")
                bulletText("Storage: %d bytes", window.stateStorage.data.size * Int.BYTES * 2)
                treePop()
            }
        }

        var showClipRects = true

        val selected = BooleanArray(4 + 3 + 16 + 16, { it == 1 || it == 23 + 0 || it == 23 + 5 || it == 23 + 10 || it == 23 + 15 })

        var styleIdx = 0
    }
}