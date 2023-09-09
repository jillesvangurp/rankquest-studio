import components.para
import components.primaryButton
import dev.fritz2.core.RenderContext
import dev.fritz2.core.href
import dev.fritz2.routing.MapRouter
import kotlinx.coroutines.flow.map

fun RenderContext.license() {
    div("mx-auto w-5/6 bg-blueBright-50 p-10") {
        h1 { +"License" }
        para {
            +"""
            The source code for Rankquest Studio is opensource and available on 
            """.trimIndent()
            a {
                +"Github"
                href("https://github.com/jillesvangurp/rankquest-studio")
            }
            +"."
        }
        pre {
            +"""
            Copyright (c) 2023, Jilles van Gurp
    
            Permission is hereby granted, free of charge, to any person obtaining
            a copy of this software and associated documentation files (the
            "Software"), to deal in the Software without restriction, including
            without limitation the rights to use, copy, modify, merge, publish,
            distribute, sublicense, and/or sell copies of the Software, and to
            permit persons to whom the Software is furnished to do so, subject to
            the following conditions:
    
            The above copyright notice and this permission notice shall be included
            in all copies or substantial portions of the Software.
    
            THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
            EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
            MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
            IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY
            CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
            TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
            SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
    
        """.trimIndent()
        }
        val router = koin.get<MapRouter>()
        primaryButton {
            +"Back"
            clicks.map { Page.About.route } handledBy router.navTo

        }
    }
}