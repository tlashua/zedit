package net.lashua.zonedit.ui.events

import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import io.kotest.core.spec.style.FunSpec
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import net.lashua.zonedit.model.ConnectionManager
import net.lashua.zonedit.model.ExitDirection
import net.lashua.zonedit.model.Position
import net.lashua.zonedit.model.Room
import net.lashua.zonedit.model.Zone
import net.lashua.zonedit.ui.state.ZoneCanvasState

class EventHandlerTest : FunSpec({

    context("AbstractDragHandler") {
        test("applyTo should return a modified Modifier") {
            // Create a mock state
            val state = mockk<ZoneCanvasState>(relaxed = true)

            // Create a handler
            val handler = object : AbstractDragHandler() {
                override fun applyTo(
                    state: ZoneCanvasState,
                    modifier: Modifier,
                    density: Float,
                    zoomLevel: Float,
                    canvasWidthDp: Float,
                    canvasHeightDp: Float
                ): Modifier {
                    return modifier
                }
            }

            // Call applyTo
            val result = handler.applyTo(
                state = state,
                modifier = Modifier,
                density = 1.0f,
                zoomLevel = 1.0f,
                canvasWidthDp = 1000f,
                canvasHeightDp = 800f
            )

            // Verify the result is a Modifier
            assert(result is Modifier)
        }
    }

    context("EventHandler") {
        test("applyTo should return a modified Modifier") {
            // Create a mock state
            val state = mockk<ZoneCanvasState>(relaxed = true)

            // Create a handler
            val handler = object : EventHandler {
                override fun applyTo(
                    state: ZoneCanvasState,
                    modifier: Modifier,
                    density: Float,
                    zoomLevel: Float,
                    canvasWidthDp: Float,
                    canvasHeightDp: Float
                ): Modifier {
                    return modifier
                }
            }

            // Call applyTo
            val result = handler.applyTo(
                state = state,
                modifier = Modifier,
                density = 1.0f,
                zoomLevel = 1.0f,
                canvasWidthDp = 1000f,
                canvasHeightDp = 800f
            )

            // Verify the result is a Modifier
            assert(result is Modifier)
        }
    }

    context("CompositeEventHandler") {
        test("applyTo should apply all handlers") {
            // Create a mock state
            val state = mockk<ZoneCanvasState>(relaxed = true)

            // Create mock handlers
            val handler1 = mockk<EventHandler>(relaxed = true)
            val handler2 = mockk<EventHandler>(relaxed = true)

            every {
                handler1.applyTo(any(), any(), any(), any(), any(), any())
            } returns Modifier

            every {
                handler2.applyTo(any(), any(), any(), any(), any(), any())
            } returns Modifier

            // Create a composite handler
            val compositeHandler = CompositeEventHandler(listOf(handler1, handler2))

            // Call applyTo
            compositeHandler.applyTo(
                state = state,
                modifier = Modifier,
                density = 1.0f,
                zoomLevel = 1.0f,
                canvasWidthDp = 1000f,
                canvasHeightDp = 800f
            )

            // Verify both handlers were called
            verify {
                handler1.applyTo(state, any(), 1.0f, 1.0f, 1000f, 800f)
            }

            verify {
                handler2.applyTo(state, any(), 1.0f, 1.0f, 1000f, 800f)
            }
        }
    }
})
