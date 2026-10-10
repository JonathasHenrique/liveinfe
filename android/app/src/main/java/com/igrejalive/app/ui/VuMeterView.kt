package com.igrejalive.app.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View

/**
 * Indicador visual de nível de áudio (VU Meter) estilo console de som.
 * Mostra barras verdes (normal), amarelas (atenção) e vermelhas (clipping).
 */
class VuMeterView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val barRect = RectF()

    // Nível atual de 0.0f a 1.0f
    private var level: Float = 0.65f
    private val totalSegments = 16

    private val greenColor = Color.parseColor("#10B981") // Tuned Green da Bíblia do Instrumentista
    private val yellowColor = Color.parseColor("#F59E0B") // Ouro Sagrado
    private val redColor = Color.parseColor("#EF4444")    // Vermelho Alerta
    private val inactiveColor = Color.parseColor("#1E2738")

    fun setAudioLevel(fraction: Float) {
        level = fraction.coerceIn(0.0f, 1.0f)
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val w = width.toFloat()
        val h = height.toFloat()
        val spacing = 3f
        val segmentHeight = (h - (spacing * (totalSegments - 1))) / totalSegments
        val activeSegments = (level * totalSegments).toInt()

        for (i in 0 until totalSegments) {
            // Desenha de baixo para cima (0 = fundo, totalSegments - 1 = topo)
            val segmentIndexFromBottom = i
            val top = h - ((i + 1) * segmentHeight + (i * spacing))
            val bottom = top + segmentHeight

            barRect.set(0f, top, w, bottom)

            val isActive = segmentIndexFromBottom < activeSegments

            if (isActive) {
                val ratio = segmentIndexFromBottom.toFloat() / totalSegments
                paint.color = when {
                    ratio > 0.85f -> redColor      // Perto de clipar (vermelho)
                    ratio > 0.65f -> yellowColor   // Bom volume (amarelo)
                    else -> greenColor             // Normal (verde)
                }
            } else {
                paint.color = inactiveColor
            }

            canvas.drawRoundRect(barRect, 2f, 2f, paint)
        }
    }
}
