package me.thimmaiah.voxbox.ui

import androidx.annotation.DrawableRes
import me.thimmaiah.voxbox.R

/**
 * The icon set, as drawable resources.
 *
 * These were hand-built `ImageVector`s in Kotlin because Material's extended icon artifact is a
 * large dependency for a handful of glyphs. They are vector drawables now for the same reason,
 * with the advantage that the geometry is data rather than code and the launcher/notification
 * paths can reference them too. Each is a 24dp stroke glyph with no baked-in tint, so
 * `Icon(painterResource(...), tint = ...)` is the only thing that colours them.
 */
object VbIcons {
    @DrawableRes val ArrowBack = R.drawable.ic_vb_arrow_back
    @DrawableRes val Camera = R.drawable.ic_vb_camera
    @DrawableRes val Check = R.drawable.ic_vb_check
    @DrawableRes val ChevronDown = R.drawable.ic_vb_chevron_down
    @DrawableRes val ChevronRight = R.drawable.ic_vb_chevron_right
    @DrawableRes val Close = R.drawable.ic_vb_close
    @DrawableRes val Cloud = R.drawable.ic_vb_cloud
    @DrawableRes val Copy = R.drawable.ic_vb_copy
    @DrawableRes val Download = R.drawable.ic_vb_download
    @DrawableRes val Edit = R.drawable.ic_vb_edit
    @DrawableRes val Flag = R.drawable.ic_vb_flag
    @DrawableRes val Folder = R.drawable.ic_vb_folder
    @DrawableRes val Home = R.drawable.ic_vb_home
    @DrawableRes val Info = R.drawable.ic_vb_info
    @DrawableRes val Library = R.drawable.ic_vb_library
    @DrawableRes val Mic = R.drawable.ic_vb_mic
    @DrawableRes val Moon = R.drawable.ic_vb_moon
    @DrawableRes val Outline = R.drawable.ic_vb_outline
    @DrawableRes val Plus = R.drawable.ic_vb_plus
    @DrawableRes val Refresh = R.drawable.ic_vb_refresh
    @DrawableRes val Search = R.drawable.ic_vb_search
    @DrawableRes val Settings = R.drawable.ic_vb_settings
    @DrawableRes val Share = R.drawable.ic_vb_share
    @DrawableRes val Shield = R.drawable.ic_vb_shield
    @DrawableRes val Stop = R.drawable.ic_vb_stop
    @DrawableRes val Sun = R.drawable.ic_vb_sun
    @DrawableRes val Trash = R.drawable.ic_vb_trash
    @DrawableRes val Waveform = R.drawable.ic_vb_waveform
    @DrawableRes val Zoom = R.drawable.ic_vb_zoom
}
