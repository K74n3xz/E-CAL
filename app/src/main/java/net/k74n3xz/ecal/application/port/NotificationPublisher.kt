package net.k74n3xz.ecal.application.port

interface NotificationPublisher {
    fun publish(id: Long, description: String)
}