package com.blockninja.counterclockwise.entity

import com.blockninja.counterclockwise.CounterClockwise
import com.blockninja.counterclockwise.create.waterwheel.WheelEntity
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.MobCategory
import net.minecraftforge.eventbus.api.IEventBus
import net.minecraftforge.registries.DeferredRegister
import net.minecraftforge.registries.ForgeRegistries
import net.minecraftforge.registries.RegistryObject
import org.valkyrienskies.mod.common.entity.VSPhysicsEntity
import java.util.function.Supplier


object CounterClockwiseEntityTypes {
    val ENTITIES: DeferredRegister<EntityType<*>> = DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, CounterClockwise.MOD_ID)

    val WATERWHEEL_ENTITY: RegistryObject<EntityType<WheelEntity>> = registerEntity(
        "waterwheel_entity"
    ) { EntityType.Builder.of(::WheelEntity, MobCategory.MISC).sized(0.1f, 0.1f).setUpdateInterval(1).sized(.3f, .3f).setTrackingRange(10).fireImmune() }

    private fun <T : Entity> registerEntity(name: String, builder: Supplier<EntityType.Builder<T>>): RegistryObject<EntityType<T>> {
        return ENTITIES.register(name, Supplier { builder.get().build(CounterClockwise.MOD_ID + ":" + name) })
    }

    fun register(eventBus: IEventBus) {
        ENTITIES.register(eventBus)
    }
}