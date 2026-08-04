package me.thimmaiah.voxbox.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathBuilder
import androidx.compose.ui.graphics.vector.PathData
import androidx.compose.ui.unit.dp

/**
 * VoxBox's own outline icon set.
 *
 * Material 3 1.4 no longer brings `material-icons-core` onto the classpath, and the extended icon
 * artifact would add a large amount of unused drawable data to an already 60 MB debug APK. These
 * are drawn on a shared 24 dp grid with one stroke weight so the whole app reads as one set. Every
 * icon is stroked in black and recoloured by `Icon`'s tint.
 */
object VoxBoxIcons {
    val Notes: ImageVector = strokeIcon("voxbox_notes") {
        rectangle(5f, 3f, 19f, 21f)
        line(8.5f, 8.5f, 15.5f, 8.5f)
        line(8.5f, 12f, 15.5f, 12f)
        line(8.5f, 15.5f, 13f, 15.5f)
    }

    val Microphone: ImageVector = strokeIcon("voxbox_microphone") {
        moveTo(9f, 5f)
        arc(3f, 15f, 5f, positive = true)
        lineTo(15f, 10f)
        arc(3f, 9f, 10f, positive = true)
        close()
        moveTo(5.5f, 11f)
        arc(6.5f, 18.5f, 11f, positive = false)
        line(12f, 17.5f, 12f, 21f)
        line(8.5f, 21f, 15.5f, 21f)
    }

    val Board: ImageVector = strokeIcon("voxbox_board") {
        rectangle(3f, 3f, 21f, 16f)
        line(12f, 16f, 12f, 20f)
        line(8f, 20f, 16f, 20f)
        line(7f, 8f, 13f, 8f)
        line(7f, 11.5f, 17f, 11.5f)
    }

    val Camera: ImageVector = strokeIcon("voxbox_camera") {
        rectangle(3f, 6.5f, 21f, 19.5f)
        line(8.5f, 6.5f, 10f, 4f)
        line(10f, 4f, 14f, 4f)
        line(14f, 4f, 15.5f, 6.5f)
        circle(12f, 13f, 3.4f)
    }

    val Back: ImageVector = strokeIcon("voxbox_back") {
        line(19f, 12f, 5f, 12f)
        moveTo(11f, 6f)
        lineTo(5f, 12f)
        lineTo(11f, 18f)
    }

    val Close: ImageVector = strokeIcon("voxbox_close") {
        line(6f, 6f, 18f, 18f)
        line(18f, 6f, 6f, 18f)
    }

    val Check: ImageVector = strokeIcon("voxbox_check") {
        moveTo(5f, 12.5f)
        lineTo(10f, 17.5f)
        lineTo(19f, 6.5f)
    }

    val Add: ImageVector = strokeIcon("voxbox_add") {
        line(12f, 5f, 12f, 19f)
        line(5f, 12f, 19f, 12f)
    }

    val Search: ImageVector = strokeIcon("voxbox_search") {
        circle(11f, 11f, 6f)
        line(15.4f, 15.4f, 20f, 20f)
    }

    val Delete: ImageVector = strokeIcon("voxbox_delete") {
        line(4f, 6.5f, 20f, 6.5f)
        moveTo(9f, 6.5f)
        lineTo(9f, 4f)
        lineTo(15f, 4f)
        lineTo(15f, 6.5f)
        moveTo(6.5f, 6.5f)
        lineTo(7.5f, 20f)
        lineTo(16.5f, 20f)
        lineTo(17.5f, 6.5f)
        line(10.5f, 10f, 10.5f, 16.5f)
        line(13.5f, 10f, 13.5f, 16.5f)
    }

    val Retry: ImageVector = strokeIcon("voxbox_retry") {
        // Three quarter sweeps leave the top-left open for the arrowhead.
        moveTo(12f, 4.5f)
        arc(7.5f, 19.5f, 12f, positive = true)
        arc(7.5f, 12f, 19.5f, positive = true)
        arc(7.5f, 4.5f, 12f, positive = true)
        moveTo(8.6f, 3.2f)
        lineTo(12f, 4.5f)
        lineTo(10.7f, 8f)
    }

    val Folder: ImageVector = strokeIcon("voxbox_folder") {
        moveTo(3f, 19f)
        lineTo(3f, 5f)
        lineTo(9.5f, 5f)
        lineTo(11.5f, 8f)
        lineTo(21f, 8f)
        lineTo(21f, 19f)
        close()
    }

    val Share: ImageVector = strokeIcon("voxbox_share") {
        moveTo(6f, 11f)
        lineTo(6f, 20f)
        lineTo(18f, 20f)
        lineTo(18f, 11f)
        line(12f, 15f, 12f, 3.5f)
        moveTo(8.5f, 7f)
        lineTo(12f, 3.5f)
        lineTo(15.5f, 7f)
    }

    val Edit: ImageVector = strokeIcon("voxbox_edit") {
        moveTo(4f, 20f)
        lineTo(4.8f, 15.6f)
        lineTo(15.8f, 4.6f)
        lineTo(19.4f, 8.2f)
        lineTo(8.4f, 19.2f)
        close()
        line(13.6f, 6.8f, 17.2f, 10.4f)
    }

    val Warning: ImageVector = strokeIcon("voxbox_warning") {
        moveTo(12f, 3.5f)
        lineTo(21.5f, 20f)
        lineTo(2.5f, 20f)
        close()
        line(12f, 9.5f, 12f, 14f)
        line(12f, 17f, 12f, 17.2f)
    }

    val Info: ImageVector = strokeIcon("voxbox_info") {
        circle(12f, 12f, 8.5f)
        line(12f, 11f, 12f, 16.5f)
        line(12f, 7.6f, 12f, 7.8f)
    }

    val ChevronRight: ImageVector = strokeIcon("voxbox_chevron_right") {
        moveTo(9.5f, 5f)
        lineTo(16.5f, 12f)
        lineTo(9.5f, 19f)
    }

    val Stop: ImageVector = strokeIcon("voxbox_stop") {
        rectangle(6f, 6f, 18f, 18f)
    }

    val Waveform: ImageVector = strokeIcon("voxbox_waveform") {
        line(3f, 10f, 3f, 14f)
        line(7f, 6.5f, 7f, 17.5f)
        line(11f, 3.5f, 11f, 20.5f)
        line(15f, 7.5f, 15f, 16.5f)
        line(19f, 10f, 19f, 14f)
    }

    val Diagram: ImageVector = strokeIcon("voxbox_diagram") {
        rectangle(3f, 4f, 10f, 10f)
        rectangle(14f, 14f, 21f, 20f)
        line(10f, 7f, 17.5f, 7f)
        line(17.5f, 7f, 17.5f, 14f)
    }
}

private fun strokeIcon(name: String, path: PathBuilder.() -> Unit): ImageVector =
    ImageVector.Builder(
        name = name,
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
    ).apply {
        addPath(
            pathData = PathData(path),
            fill = null,
            stroke = SolidColor(Color.Black),
            strokeLineWidth = 1.7f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
        )
    }.build()

private fun PathBuilder.line(x1: Float, y1: Float, x2: Float, y2: Float) {
    moveTo(x1, y1)
    lineTo(x2, y2)
}

private fun PathBuilder.rectangle(left: Float, top: Float, right: Float, bottom: Float) {
    moveTo(left, top)
    lineTo(right, top)
    lineTo(right, bottom)
    lineTo(left, bottom)
    close()
}

/** Two half sweeps in the same direction, which draws the same ring whichever way it travels. */
private fun PathBuilder.circle(centerX: Float, centerY: Float, radius: Float) {
    moveTo(centerX - radius, centerY)
    arcTo(radius, radius, 0f, isMoreThanHalf = false, isPositiveArc = true, centerX + radius, centerY)
    arcTo(radius, radius, 0f, isMoreThanHalf = false, isPositiveArc = true, centerX - radius, centerY)
}

/**
 * A single sweep to [x]/[y]. `positive = true` renders clockwise on screen, matching the SVG
 * sweep flag, so the bulge direction of an open arc is explicit at each call site.
 */
private fun PathBuilder.arc(radius: Float, x: Float, y: Float, positive: Boolean) {
    arcTo(radius, radius, 0f, isMoreThanHalf = false, isPositiveArc = positive, x, y)
}
