package net.k74n3xz.ecal.core.application.port.outbound.platform

interface NotificationPublisher {
    fun publish(id: Long, description: String): Boolean
}
