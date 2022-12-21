package dev.mtpeter.rsqrecruitmenttask.configuration

import io.kotest.core.config.AbstractProjectConfig
import io.kotest.core.extensions.Extension
import io.kotest.core.spec.IsolationMode
import io.kotest.extensions.spring.SpringExtension

class KotestConfig : AbstractProjectConfig() {

    override val isolationMode: IsolationMode
        get() = IsolationMode.InstancePerTest

    override fun extensions(): List<Extension> = listOf(SpringExtension)
}