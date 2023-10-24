package searchpluginconfig

import com.jilesvangurp.rankquest.core.pluginconfiguration.SearchPluginConfiguration
import components.LocalStoringStore
import components.confirm
import koin
import kotlinx.coroutines.Job
import kotlinx.serialization.builtins.ListSerializer

class PluginConfigurationsStore : LocalStoringStore<List<SearchPluginConfiguration>>(
    listOf(), "plugin-configurations", ListSerializer(SearchPluginConfiguration.serializer())
) {
    private val activeSearchPluginConfigurationStore = koin.get<ActiveSearchPluginConfigurationStore>()

    val addOrReplace = handle<SearchPluginConfiguration> { _, config ->
        (current ?: listOf()).map {
            if (it.id == config.id) {
                config
            } else {
                it
            }
        }.let { configurations ->
            if (configurations.firstOrNull { it.id == config.id } == null) {
                configurations + config
            } else {
                configurations
            }
        }.also { newConfigs ->
            val active = activeSearchPluginConfigurationStore.current
            newConfigs.forEach {
                if (it.id == active?.id) {
                    activeSearchPluginConfigurationStore.update(it)
                }
            }
        }
    }
    val remove = handle<String> { old, id ->
        confirm(job=job) {
            update((current ?: listOf()).filter { it.id != id })
            if (activeSearchPluginConfigurationStore.current?.id == id) {
                activeSearchPluginConfigurationStore.update(null)
            }
        }
        old
    }
}