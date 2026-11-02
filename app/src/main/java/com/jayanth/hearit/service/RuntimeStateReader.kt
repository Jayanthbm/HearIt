package com.jayanth.hearit.service

import android.app.KeyguardManager
import android.app.NotificationManager
import android.content.Context
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.PowerManager
import com.jayanth.hearit.core.common.model.AudioOutput
import com.jayanth.hearit.core.common.model.AudioOutputKind
import com.jayanth.hearit.core.common.model.RuntimeState

/**
 * Reads the device state the rules pipeline needs, without requesting any permission
 * (P06 decision: no dangerous permissions before the setup flow explains them).
 *
 * All readings are best-effort and never throw; each has a documented, safe fallback:
 *  - **screen** via [PowerManager.isInteractive];
 *  - **lock** via [KeyguardManager.isKeyguardLocked];
 *  - **call** via [AudioManager.getMode] `IN_CALL`/`IN_COMMUNICATION` — a permission-free
 *    proxy for "a call is in progress"; exact telephony state needs `READ_PHONE_STATE`,
 *    which is not requested in v1 (master prompt §6 only needs "never speak during a call");
 *  - **DND** via [NotificationManager.getCurrentInterruptionFilter] — anything other than
 *    `ALL` counts as DND; a silent channel is never treated as DND (ADR-017);
 *  - **mute** from [SessionStore] (P16 owns the control);
 *  - **active output** from connected output devices: Bluetooth first, then wired, then
 *    the phone speaker; an unclassifiable/absent output yields `null`, which the rules
 *    treat as allowed (ADR-017).
 *
 * Priority Announce state is passed in: its toggle + expiry belong to the service/feature
 * layer (P17). Not locally verified — cloud-agent restriction.
 */
class RuntimeStateReader(
    context: Context,
    private val audioManager: AudioManager,
    private val sessionStore: SessionStore,
) {

    private val powerManager = context.getSystemService(PowerManager::class.java)
    private val keyguardManager = context.getSystemService(KeyguardManager::class.java)
    private val notificationManager = context.getSystemService(NotificationManager::class.java)

    /** Snapshot of the current device state for one rules evaluation. */
    fun read(priorityAnnounceActive: Boolean = false): RuntimeState = RuntimeState(
        screenOn = powerManager?.isInteractive ?: true,
        isLocked = keyguardManager?.isKeyguardLocked ?: false,
        temporaryMuteActive = sessionStore.isTemporaryMuteActive,
        priorityAnnounceActive = priorityAnnounceActive,
        isCallActive = audioManager.mode == AudioManager.MODE_IN_CALL ||
            audioManager.mode == AudioManager.MODE_IN_COMMUNICATION,
        isDndActive = (notificationManager?.currentInterruptionFilter ?: DND_OFF) != DND_OFF,
        activeOutput = readActiveOutput(),
    )

    /** Best connected output, in preference order; null when nothing is observed. */
    private fun readActiveOutput(): AudioOutput? {
        val devices = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)

        fun firstOf(vararg types: Int): AudioDeviceInfo? =
            devices.firstOrNull { device -> types.any { it == device.type } }

        firstOf(
            AudioDeviceInfo.TYPE_BLUETOOTH_A2DP,
            AudioDeviceInfo.TYPE_BLUETOOTH_SCO,
        )?.let { return it.toOutput(AudioOutputKind.BLUETOOTH) }

        firstOf(
            AudioDeviceInfo.TYPE_WIRED_HEADSET,
            AudioDeviceInfo.TYPE_WIRED_HEADPHONES,
            AudioDeviceInfo.TYPE_USB_HEADSET,
        )?.let { return it.toOutput(AudioOutputKind.WIRED) }

        firstOf(
            AudioDeviceInfo.TYPE_BUILTIN_SPEAKER,
            AudioDeviceInfo.TYPE_BUILTIN_EARPIECE,
        )?.let { return it.toOutput(AudioOutputKind.PHONE_SPEAKER) }

        return null
    }

    private fun AudioDeviceInfo.toOutput(kind: AudioOutputKind): AudioOutput = AudioOutput(
        kind = kind,
        id = address?.takeIf { it.isNotBlank() },
        name = productName?.toString(),
    )

    private companion object {
        /** `INTERRUPTION_FILTER_ALL` means "do not suppress anything" — DND is off. */
        const val DND_OFF = NotificationManager.INTERRUPTION_FILTER_ALL
    }
}
