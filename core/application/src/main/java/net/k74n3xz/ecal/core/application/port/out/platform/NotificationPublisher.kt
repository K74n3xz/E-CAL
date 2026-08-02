package net.k74n3xz.ecal.core.application.port.out.platform

interface NotificationPublisher {
    fun publish(id: Long, description: String): Boolean
}
