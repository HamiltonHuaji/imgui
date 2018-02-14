package imgui

import glm_.b
import glm_.i
import imgui.imgui.*

/** -----------------------------------------------------------------------------
 *      Context
 *  -----------------------------------------------------------------------------
 *
 *  Current context pointer. Implicitely used by all ImGui functions. Always assumed to be != null.
 *  ::createContext() will automatically set this pointer if it is null. Change to a different context by calling
 *  ::setCurrentContext().
 *  If you use DLL hotreloading you might need to call ::setCurrentContext() after reloading code from this file.
 *  ImGui functions are not thread-safe because of this pointer. If you want thread-safety to allow N threads to access
 *  N different contexts, you can:
 *      - Change this variable to use thread local storage. You may #define GImGui in imconfig.h for that purpose.
 *          Future development aim to make this context pointer explicit to all calls.
 *          Also read https://github.com/ocornut/imgui/issues/586
 *      - Having multiple instances of the ImGui code compiled inside different namespace (easiest/safest, if you have
 *          a finite number of contexts)    */
val g get() = gImGui!!
var gImGui: Context? = null

// Helpers macros to generate 32-bits encoded colors
var USE_BGRA_PACKED_COLOR = false

val COL32_R_SHIFT by lazy { if (USE_BGRA_PACKED_COLOR) 16 else 0 }
val COL32_G_SHIFT = 8
val COL32_B_SHIFT by lazy { if (USE_BGRA_PACKED_COLOR) 0 else 16 }
val COL32_A_SHIFT = 24
val COL32_A_MASK = 0xFF000000.i

fun COL32(r: Int, g: Int, b: Int, a: Int) = (a shl COL32_A_SHIFT) or (b shl COL32_B_SHIFT) or (g shl COL32_G_SHIFT) or (r shl COL32_R_SHIFT)
val COL32_WHITE = COL32(255, 255, 255, 255) // Opaque white = 0xFFFFFFFF
val COL32_BLACK = COL32(0, 0, 0, 255)       // Opaque black
val COL32_BLACK_TRANS = COL32(0, 0, 0, 0)   // Transparent black = 0x00000000

val MOUSE_INVALID = -256000f

var IMGUI_HAS_NAV = false   // TODO remove
val IMGUI_DEBUG_NAV_SCORING = false
val IMGUI_DEBUG_NAV_RECTS = false

object ImGui :

        imgui_main,
        imgui_demoDebugInformations,
        imgui_window,
        imgui_parametersStacks,
        imgui_cursorLayout,
        imgui_colums,
        imgui_idScopes,
        imgui_widgetsText,
        imgui_widgetsMain,
        imgui_widgetsComboBox,
        imgui_widgetsDrag,
        imgui_widgetsInputKeyboard,
        imgui_widgetsSliders,
        imgui_widgetsColorEditorPicker,
        imgui_widgetsTrees,
        imgui_widgetsSelectableLists,
        imgui_tooltips,
        imgui_menus,
        imgui_popups,
        imgui_logging,
        imgui_dragAndDrop,
        imgui_clipping,
        imgui_styles,
        imgui_focusActivation,
        imgui_utilities,
        imgui_inputs,
        imgui_helpers,

        imgui_internal {

    val version = "1.54 WIP"
}

var ptrIndices = 0
var ptrId = Array(512, { java.lang.Byte(it.b) })

// TODO get rid of local top value KMutableProperty in favor of the better with*{} solution

typealias SizeCallback = (SizeCallbackData) -> Unit

// dummy main
fun main(args: Array<String>) {

}

val NUL = '\u0000'

var DEBUG = true

operator fun StringBuilder.plusAssign(string: String) {
    append(string)
}