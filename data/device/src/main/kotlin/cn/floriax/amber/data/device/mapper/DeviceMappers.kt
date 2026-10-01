package cn.floriax.amber.data.device.mapper

import cn.floriax.amber.core.database.entity.ClockDeviceEntity
import cn.floriax.amber.domain.device.model.ClockDevice

/**
 * Device row ↔ domain model. Column names live in the entity; this
 * mapper is the only place that knows both sides.
 *
 * @author WangZhiYao
 * @since 2026/9/30
 */
fun ClockDevice.toEntity(): ClockDeviceEntity = ClockDeviceEntity(
    mac = mac,
    alias = alias,
    advertisedName = advertisedName,
    isDefault = isDefault,
    lastConnectedAt = lastConnectedAt,
)

/** Row → domain model. */
fun ClockDeviceEntity.toClockDevice(): ClockDevice = ClockDevice(
    mac = mac,
    alias = alias,
    advertisedName = advertisedName,
    isDefault = isDefault,
    lastConnectedAt = lastConnectedAt,
)
