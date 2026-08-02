package net.k74n3xz.ecal.core.application.port.out.platform

interface AudioPlayer {
    fun play(attachmentId: Long?): Boolean
}
