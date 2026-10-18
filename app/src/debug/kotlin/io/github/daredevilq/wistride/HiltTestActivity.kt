package io.github.daredevilq.wistride

import androidx.activity.ComponentActivity
import dagger.hilt.android.AndroidEntryPoint

/** Empty Hilt-enabled host for Compose tests; debug-only so it never ships in release. */
@AndroidEntryPoint
class HiltTestActivity : ComponentActivity()
