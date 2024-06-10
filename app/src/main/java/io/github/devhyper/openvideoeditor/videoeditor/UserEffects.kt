package io.github.devhyper.openvideoeditor.videoeditor

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.RotateRight
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.Filter
import androidx.compose.material.icons.filled.InvertColors
import androidx.media3.effect.RgbFilter
import androidx.media3.effect.ScaleAndRotateTransformation
import io.github.devhyper.openvideoeditor.R
import io.github.devhyper.openvideoeditor.misc.validateFloat
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

val userEffectsArray: ImmutableList<UserEffect> = persistentListOf(
    UserEffect(
        R.string.grayscale,
        { Icons.Filled.Filter }) { RgbFilter.createGrayscaleFilter() },
    UserEffect(
        R.string.invert_colors,
        { Icons.Filled.InvertColors }) { RgbFilter.createInvertedFilter() }//RgbFilter.createInvertedFilter()
)

val dialogUserEffectsArray: ImmutableList<DialogUserEffect> = persistentListOf(

    DialogUserEffect(
        R.string.rotate,
        { Icons.AutoMirrored.Filled.RotateRight },
        persistentListOf(
            EffectDialogSetting(
                key = "Degrees",
                stringResId = R.string.degrees,
                textfieldValidation = {
                    validateFloat(it)
                }
            )
        )
    ) { args ->
        val degrees = args["Degrees"]!!.toFloat();
        { ScaleAndRotateTransformation.Builder().setRotationDegrees(degrees).build() }
    }
)

val onVideoUserEffectsArray: ImmutableList<OnVideoUserEffect> = persistentListOf(
    /*   OnVideoUserEffect(
           R.string.text,
           { Icons.Filled.TextFormat }
       ) { TextEditor(it) },*/
    OnVideoUserEffect(
        R.string.crop,
        { Icons.Filled.Crop }
    ) { CropEditor(it) }
)
