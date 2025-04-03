package net.lashua.zonedit.util

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import net.lashua.zonedit.model.Position
import net.lashua.zonedit.model.Room
import net.lashua.zonedit.model.Zone

class CoordinateConverterTest : FunSpec({

    // Test constants
    val density = 2.0f
    val zoomLevel = 1.5f

    context("Coordinate conversion") {
        test("modelToScreen should convert model coordinates to screen coordinates") {
            val modelPosition = Position(x = 100f, y = 50f)

            val screenPosition = CoordinateConverter.modelToScreen(modelPosition, density, zoomLevel)

            // Expected: model * density * zoom
            screenPosition.x shouldBe 100f * density * zoomLevel
            screenPosition.y shouldBe 50f * density * zoomLevel
        }

        test("screenToModel should convert screen coordinates to model coordinates") {
            val screenPosition = Offset(x = 300f, y = 150f)

            val modelPosition = CoordinateConverter.screenToModel(screenPosition, density, zoomLevel)

            // Expected: screen / (density * zoom)
            modelPosition.x shouldBe 300f / (density * zoomLevel)
            modelPosition.y shouldBe 150f / (density * zoomLevel)
        }

        test("conversion should be reversible") {
            val originalPosition = Position(x = 100f, y = 50f)

            val screenPosition = CoordinateConverter.modelToScreen(originalPosition, density, zoomLevel)
            val roundTripPosition = CoordinateConverter.screenToModel(screenPosition, density, zoomLevel)

            // Due to floating point precision, we use approximate equality
            roundTripPosition.x shouldBe originalPosition.x
            roundTripPosition.y shouldBe originalPosition.y
        }
    }

    context("Drag conversion") {
        test("screenDragToModelDrag should convert screen drag to model drag") {
            val screenDrag = Offset(x = 30f, y = 20f)

            val modelDrag = CoordinateConverter.screenDragToModelDrag(screenDrag, density, zoomLevel)

            // Expected: screen / (density * zoom)
            modelDrag.x shouldBe 30f / (density * zoomLevel)
            modelDrag.y shouldBe 20f / (density * zoomLevel)
        }
    }

    context("Room rectangle calculation") {
        test("getRoomRect should create correct rectangle for a room") {
            val room = Room(
                id = "test0",
                name = "Test Room",
                description = "Test room",
                position = Position(x = 100f, y = 50f)
            )

            val zone = Zone(
                id = "test",
                name = "Test Zone",
                nodeWidthDp = 120f,
                nodeHeightDp = 80f
            )

            val rect = CoordinateConverter.getRoomRect(room, zone, density, zoomLevel)

            // Expected position: model position * density * zoom
            val expectedX = 100f * density * zoomLevel
            val expectedY = 50f * density * zoomLevel

            // Expected size: node dimensions * density * zoom
            val expectedWidth = 120f * density * zoomLevel
            val expectedHeight = 80f * density * zoomLevel

            rect.left shouldBe expectedX
            rect.top shouldBe expectedY
            rect.right shouldBe expectedX + expectedWidth
            rect.bottom shouldBe expectedY + expectedHeight
        }
    }

    context("Point containment") {
        test("containsPoint should detect when a point is inside a rectangle") {
            val rect = Rect(left = 100f, top = 50f, right = 200f, bottom = 100f)

            // Point inside
            CoordinateConverter.containsPoint(rect, Offset(150f, 75f)) shouldBe true

            // Point outside
            CoordinateConverter.containsPoint(rect, Offset(50f, 75f)) shouldBe false

            // Point on edge (with default tolerance)
            CoordinateConverter.containsPoint(rect, Offset(100f, 75f)) shouldBe true

            // Point slightly outside (within tolerance)
            CoordinateConverter.containsPoint(rect, Offset(99.6f, 75f), tolerance = 0.5f) shouldBe true

            // Point slightly outside (beyond tolerance)
            CoordinateConverter.containsPoint(rect, Offset(99f, 75f), tolerance = 0.5f) shouldBe false
        }
    }

    context("Zoom scaling") {
        test("scaleWithZoom should scale values based on zoom level") {
            val size = 10f

            val scaledSize = CoordinateConverter.scaleWithZoom(size, zoomLevel)

            scaledSize shouldBe size * zoomLevel
        }

        test("scaleWithZoom should handle zero zoom level") {
            val size = 10f
            val zeroZoom = 0f

            val scaledSize = CoordinateConverter.scaleWithZoom(size, zeroZoom)

            scaledSize shouldBe 0f
        }

        test("scaleWithZoom should handle negative values") {
            val size = -10f

            val scaledSize = CoordinateConverter.scaleWithZoom(size, zoomLevel)

            scaledSize shouldBe size * zoomLevel
        }
    }

    context("Edge cases") {
        test("modelToScreen and screenToModel should handle zero values") {
            val zeroPosition = Position(0f, 0f)
            val zeroOffset = Offset(0f, 0f)

            val screenPos = CoordinateConverter.modelToScreen(zeroPosition, density, zoomLevel)
            val modelPos = CoordinateConverter.screenToModel(zeroOffset, density, zoomLevel)

            screenPos shouldBe zeroOffset
            modelPos shouldBe zeroPosition
        }

        test("modelToScreen and screenToModel should handle negative values") {
            val negativePosition = Position(-100f, -50f)
            val negativeOffset = Offset(-300f, -150f)

            val screenPos = CoordinateConverter.modelToScreen(negativePosition, density, zoomLevel)
            val modelPos = CoordinateConverter.screenToModel(negativeOffset, density, zoomLevel)

            screenPos.x shouldBe negativePosition.x * density * zoomLevel
            screenPos.y shouldBe negativePosition.y * density * zoomLevel

            modelPos.x shouldBe negativeOffset.x / (density * zoomLevel)
            modelPos.y shouldBe negativeOffset.y / (density * zoomLevel)
        }

        test("containsPoint should handle points exactly on the edge") {
            val rect = Rect(left = 100f, top = 50f, right = 200f, bottom = 100f)

            // Points exactly on edges
            CoordinateConverter.containsPoint(rect, Offset(100f, 50f)) shouldBe true  // Top-left corner
            CoordinateConverter.containsPoint(rect, Offset(200f, 50f)) shouldBe true  // Top-right corner
            CoordinateConverter.containsPoint(rect, Offset(100f, 100f)) shouldBe true // Bottom-left corner
            CoordinateConverter.containsPoint(rect, Offset(200f, 100f)) shouldBe true // Bottom-right corner
            CoordinateConverter.containsPoint(rect, Offset(150f, 50f)) shouldBe true  // Top edge
            CoordinateConverter.containsPoint(rect, Offset(150f, 100f)) shouldBe true // Bottom edge
            CoordinateConverter.containsPoint(rect, Offset(100f, 75f)) shouldBe true  // Left edge
            CoordinateConverter.containsPoint(rect, Offset(200f, 75f)) shouldBe true  // Right edge
        }
    }
})
