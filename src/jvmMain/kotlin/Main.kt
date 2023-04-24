import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import kotlin.math.roundToInt


fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "Compose for Desktop",
        state = rememberWindowState(width = 1500.dp, height = 1000.dp)
    ) {
        Scaffold(

        ) {
            val count = remember { mutableStateOf(0) }
            MaterialTheme {
                Column(Modifier.fillMaxSize(), Arrangement.spacedBy(5.dp)) {
                    Button(modifier = Modifier.align(Alignment.CenterHorizontally),
                        onClick = {
                            count.value++
                        }) {
                        Text(if (count.value == 0) "Hello World" else "Clicked ${count.value}!")
                    }
                    Button(modifier = Modifier.align(Alignment.CenterHorizontally),
                        onClick = {
                            count.value = 0
                        }) {
                        Text("Reset")
                    }


                    DraggableText()
                    DraggableBox()

//                    Spacer(
//                        modifier = Modifier
//                            .drawWithCache {
//                                val path = Path()
//                                path.moveTo(0f, 0f)
//                                path.lineTo(size.width / 2f, size.height / 2f)
//                                path.lineTo(size.width, 0f)
//                                path.close()
//                                onDrawBehind {
//                                    drawPath(path, Color.Gray, style = Stroke(width = 10f))
//                                }
//                            }
//                            .fillMaxSize()
//                    )
//                RectangleShapeDemo()
                }

//            MapCanvas()
//            RectangleShapeDemo()
            }
        }
    }
}

@Composable
private fun DraggableBox() {
//    Box(modifier = Modifier.fillMaxSize()) {
        var offsetX by remember { mutableStateOf(0f) }
        var offsetY by remember { mutableStateOf(0f) }

        Box(
            Modifier.offset { IntOffset(offsetX.roundToInt(), offsetY.roundToInt()) }
                .background(Color(0x76b5c5))
//                .size(50.dp)
                .pointerInput(Unit) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        offsetX += dragAmount.x
                        offsetY += dragAmount.y
                    }
                }
        ) {
            Text("Text in a box")
        }
//    }
}

@Composable
private fun DraggableText() {
    var offsetX by remember { mutableStateOf(0f) }
    Text(
        modifier = Modifier
            .offset { IntOffset(offsetX.roundToInt(), 0) }
            .draggable(
                orientation = Orientation.Horizontal,
                state = rememberDraggableState { delta ->
                    offsetX += delta
                }
            ),
        text = "Drag me!"
    )
}

@Composable
fun MapCanvas() {
    var offsetX by remember { mutableStateOf(0f) }
    var offsetY by remember { mutableStateOf(0f) }
    var offsetX1 by remember { mutableStateOf(0f) }
    var offsetY1 by remember { mutableStateOf(0f) }

    Canvas(modifier = Modifier.fillMaxSize()
        .pointerInput(Unit) {
            detectDragGestures { change, dragAmount ->
                change.consume()
                offsetX += dragAmount.x
                offsetY += dragAmount.y

                offsetX1 += dragAmount.x * 1.5f
                offsetY1 += dragAmount.y * 3f
            }
        }
    ) {
        val canvasQuadrantSize = size / 6F
        drawRect(
            topLeft = Offset(offsetX, offsetY),
            color = Color.Green,
            size = canvasQuadrantSize,
        )

        val canvasQuadrantSize2 = size / 6F / 2f
        drawRect(
            topLeft = Offset(offsetX1, offsetY1),
            color = Color.Blue,
            size = canvasQuadrantSize2
        )


//        RectangleShapeDemo()

//        MyUI()
    }
}

@Composable
fun Hello(text: String) {
    Text(
        text,
        modifier = Modifier
            .drawBehind {
                drawRoundRect(
                    Color(0xFFBBAAEE),
                    cornerRadius = CornerRadius(10.dp.toPx())
                )
            }
            .padding(4.dp)
    )
}

@Composable
fun RectangleShapeDemo() {
    ExampleBox(shape = RectangleShape)
}

@Composable
fun ExampleBox(shape: Shape) {
    Column(modifier = Modifier.fillMaxWidth().wrapContentSize(Alignment.Center)) {
        Box(
            modifier = Modifier.size(100.dp).clip(shape).background(Color.Red)
        )
    }
}

@Composable
fun MyUI() {
    Box(
        modifier = Modifier
            .size(size = 100.dp)
            .clip(shape = RectangleShape)
            .background(color = Color.Green)
    ) {

    }
}


