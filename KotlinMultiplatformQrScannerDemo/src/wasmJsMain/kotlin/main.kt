import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import kotlinx.browser.document
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.w3c.dom.HTMLDivElement
import org.w3c.dom.HTMLScriptElement

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    ComposeViewport(document.body!!) {

        val script = document.createElement("script") as HTMLScriptElement
        script.src = "https://unpkg.com/html5-qrcode"
        script.type = "text/javascript"
        document.body?.appendChild(script)

        App()
    }
}