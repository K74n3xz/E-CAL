package net.k74n3xz.ecal.core.application.port.outbound.platform

interface AudioPlayer {
    fun play(attachmentId: Long?): Boolean
}
