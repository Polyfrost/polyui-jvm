package org.polyfrost.polyui.data.image

class FlagKey<T>(val name: String) {
    override fun toString(): String = "FlagKey($name)"

    override fun hashCode(): Int = name.hashCode()

    override fun equals(other: Any?): Boolean {
        if (other === this) return true
        if (other !is FlagKey<*>) return false

        return name == other.name
    }
}
