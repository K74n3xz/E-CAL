package net.k74n3xz.ecal.core.application.port.outbound.platform

interface EmailSender {
    fun send(id: Int, subject: String, text: String, receivers: List<String>, attachments: List<String>): Boolean
}
