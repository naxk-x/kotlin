/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.interpreter.state.reflection

import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrTypeParameter
import org.jetbrains.kotlin.ir.interpreter.IrInterpreter
import org.jetbrains.kotlin.ir.interpreter.proxy.reflection.KClassProxy
import org.jetbrains.kotlin.ir.interpreter.proxy.reflection.KTypeParameterProxy
import org.jetbrains.kotlin.ir.interpreter.proxy.reflection.KTypeProxy
import org.jetbrains.kotlin.ir.interpreter.renderType
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.types.Variance
import kotlin.reflect.KClassifier
import kotlin.reflect.KTypeProjection

internal class KTypeState(val irType: IrType, override val irClass: IrClass) : ReflectionState() {
    private var _classifier: KClassifier? = null
    private var _arguments: List<KTypeProjection>? = null

    fun getClassifier(interpreter: IrInterpreter): KClassifier? {
        if (_classifier != null) return _classifier!!
        _classifier = when (val classifier = irType.classifierOrFail.owner) {
            is IrClass -> KClassProxy(KClassState(classifier, interpreter.irBuiltIns.kClassClass.owner), interpreter)
            is IrTypeParameter -> {
                val kTypeParameterIrClass = interpreter.irBuiltIns.kClassClass.owner.getIrClassOfReflectionFromList("typeParameters")
                KTypeParameterProxy(KTypeParameterState(classifier, kTypeParameterIrClass), interpreter)
            }
            else -> TODO()
        }
        return _classifier!!
    }

    fun getArguments(interpreter: IrInterpreter): List<KTypeProjection> {
        if (_arguments != null) return _arguments!!
        _arguments = (irType as IrSimpleType).arguments
            .map {
                when (it.getVariance()) {
                    Variance.INVARIANT -> KTypeProjection.invariant(KTypeProxy(KTypeState(it.typeOrNull!!, irClass), interpreter))
                    Variance.IN_VARIANCE -> KTypeProjection.contravariant(KTypeProxy(KTypeState(it.typeOrNull!!, irClass), interpreter))
                    Variance.OUT_VARIANCE -> KTypeProjection.covariant(KTypeProxy(KTypeState(it.typeOrNull!!, irClass), interpreter))
                    null -> KTypeProjection.STAR
                }
            }
        return _arguments!!
    }

    private fun IrTypeArgument.getVariance(): Variance? {
        return when (this) {
            is IrSimpleType -> Variance.INVARIANT
            is IrTypeProjection -> this.variance
            is IrStarProjection -> null
            else -> TODO()
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as KTypeState

        if (irType != other.irType) return false

        return true
    }

    override fun hashCode(): Int {
        return irType.hashCode()
    }

    override fun toString(): String {
        return irType.renderType()
    }
}