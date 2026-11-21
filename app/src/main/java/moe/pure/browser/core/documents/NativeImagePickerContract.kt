package moe.pure.browser.core.documents

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.contract.ActivityResultContracts

/** x5.e is registered by each Fragment, so a photo result belongs to that editor's lifecycle. */
internal class NativeImagePickerContract : ActivityResultContract<Void?, Uri?>() {
    private val photos = if (Build.VERSION.SDK_INT >= 36) ActivityResultContracts.PickVisualMedia() else null
    override fun createIntent(context: Context, input: Void?): Intent = photos?.createIntent(context,
        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        ?: Intent(Intent.ACTION_PICK).setType("image/*")
    override fun parseResult(resultCode: Int, intent: Intent?): Uri? = if (photos != null)
        photos.parseResult(resultCode, intent) else if (resultCode == Activity.RESULT_OK) intent?.data else null
}
