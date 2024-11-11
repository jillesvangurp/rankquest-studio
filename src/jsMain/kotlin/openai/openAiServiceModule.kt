package openai

import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

val openAiServiceModule = module {
    singleOf(::OpenAiService)
}

