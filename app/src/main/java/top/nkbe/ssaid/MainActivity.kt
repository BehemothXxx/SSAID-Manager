package top.nkbe.ssaid

import android.app.Activity
import android.app.AlertDialog
import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.graphics.Typeface
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowInsets
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

class MainActivity : Activity() {

    private enum class AppFilter {
        ALL,
        USER,
        SYSTEM,
        MODIFIED
    }

    private lateinit var rootRepository: SsaidRootRepository
    private lateinit var historyStore: SsaidHistoryStore
    private val backgroundExecutor = Executors.newSingleThreadExecutor()
    private val operationRunning = AtomicBoolean(false)
    private var activeSuExecutable: String? = null
    private var loadedEntries: List<SsaidEntry> = emptyList()
    private var currentFilterQuery: String = ""
    private var currentFilterType: AppFilter = AppFilter.ALL

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        rootRepository = SsaidRootRepository()
        historyStore = SsaidHistoryStore(this)

        applyWindowInsets()
        bindActions()
        loadBasicInformation()
    }

    override fun onDestroy() {
        backgroundExecutor.shutdownNow()
        super.onDestroy()
    }

    private fun bindActions() {
        findViewById<ImageButton>(R.id.rebootButton).setOnClickListener {
            openRebootMenu()
        }
        findViewById<ImageButton>(R.id.refreshButton).setOnClickListener {
            loadBasicInformation()
            activeSuExecutable?.let { su -> loadSsaidEntries(su, showLoading = false) }
        }
        findViewById<TextView>(R.id.copyCurrentSsaidButton).setOnClickListener {
            copyIdentifier(R.string.ssaid_current_label, R.id.currentSsaidValue)
        }
        findViewById<TextView>(R.id.copyDeviceInfoButton).setOnClickListener {
            copyIdentifier(R.string.device_info_label, R.id.deviceInfoValue)
        }
        findViewById<Button>(R.id.manageSsaidButton).setOnClickListener {
            activeSuExecutable?.let { su ->
                loadSsaidEntries(su, showLoading = true)
            } ?: openRootRequestDialog()
        }
        findViewById<TextView>(R.id.githubLink).setOnClickListener {
            openGithub()
        }

        val searchInput = findViewById<EditText>(R.id.searchEditText)
        val clearButton = findViewById<ImageView>(R.id.clearSearchButton)
        searchInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                currentFilterQuery = s?.toString()?.trim().orEmpty()
                clearButton.visibility = if (currentFilterQuery.isNotEmpty()) View.VISIBLE else View.GONE
                applyFilterAndRender()
            }
            override fun afterTextChanged(s: Editable?) {}
        })
        clearButton.setOnClickListener {
            searchInput.setText("")
        }

        findViewById<TextView>(R.id.chipFilterAll).setOnClickListener {
            setAppFilter(AppFilter.ALL)
        }
        findViewById<TextView>(R.id.chipFilterUser).setOnClickListener {
            setAppFilter(AppFilter.USER)
        }
        findViewById<TextView>(R.id.chipFilterSystem).setOnClickListener {
            setAppFilter(AppFilter.SYSTEM)
        }
        findViewById<TextView>(R.id.chipFilterModified).setOnClickListener {
            setAppFilter(AppFilter.MODIFIED)
        }
    }

    private fun setAppFilter(filter: AppFilter) {
        if (currentFilterType == filter) return
        currentFilterType = filter
        updateFilterChipsUi()
        applyFilterAndRender()
    }

    private fun updateFilterChipsUi() {
        val chipAll = findViewById<TextView>(R.id.chipFilterAll)
        val chipUser = findViewById<TextView>(R.id.chipFilterUser)
        val chipSystem = findViewById<TextView>(R.id.chipFilterSystem)
        val chipModified = findViewById<TextView>(R.id.chipFilterModified)

        val chips = listOf(
            AppFilter.ALL to chipAll,
            AppFilter.USER to chipUser,
            AppFilter.SYSTEM to chipSystem,
            AppFilter.MODIFIED to chipModified
        )

        for ((filterType, chipView) in chips) {
            val isSelected = filterType == currentFilterType
            chipView.setBackgroundResource(
                if (isSelected) R.drawable.bg_filter_chip_selected
                else R.drawable.bg_filter_chip_unselected
            )
            chipView.setTextColor(
                if (isSelected) getColor(R.color.primary)
                else getColor(R.color.on_surface_variant)
            )
            chipView.setTypeface(null, if (isSelected) Typeface.BOLD else Typeface.NORMAL)
        }
    }

    private fun isSystemApp(packageName: String): Boolean = try {
        val appInfo = packageManager.getApplicationInfo(packageName, PackageManager.GET_META_DATA)
        (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0 ||
            (appInfo.flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0
    } catch (_: PackageManager.NameNotFoundException) {
        false
    }

    private fun hasHistory(packageName: String): Boolean =
        historyStore.records(packageName).isNotEmpty()

    private fun openGithub() {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.github_url)))
        try {
            startActivity(intent)
        } catch (_: ActivityNotFoundException) {
            showMessage(getString(R.string.open_github_failed))
        }
    }

    @Suppress("HardwareIds")
    private fun loadBasicInformation() {
        findViewById<TextView>(R.id.currentSsaidValue).text = currentSsaid()
        findViewById<TextView>(R.id.deviceInfoValue).text = deviceInfo()
        if (activeSuExecutable == null) {
            updateStatusDot(R.color.status_disabled)
            findViewById<TextView>(R.id.ssaidRootStatus).setText(R.string.ssaid_root_hint)
            findViewById<TextView>(R.id.ssaidEmptyState).setText(R.string.ssaid_list_empty)
            findViewById<View>(R.id.ssaidEmptyState).visibility = View.VISIBLE
            findViewById<View>(R.id.ssaidListContainer).visibility = View.GONE
            findViewById<View>(R.id.searchContainer).visibility = View.GONE
            findViewById<View>(R.id.filterChipsScroll).visibility = View.GONE
            findViewById<View>(R.id.ssaidCountBadge).visibility = View.GONE
        }
    }

    @Suppress("HardwareIds")
    private fun currentSsaid(): String =
        Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID)
            ?.takeUnless { it.isBlank() }
            ?: getString(R.string.unavailable)

    private fun deviceInfo(): String = getString(
        R.string.device_info_format,
        Build.MANUFACTURER,
        Build.BRAND,
        Build.MODEL,
        Build.VERSION.RELEASE,
        Build.VERSION.SDK_INT,
        Build.FINGERPRINT
    )

    @Suppress("DEPRECATION")
    private fun applyWindowInsets() {
        val main = findViewById<View>(R.id.main)
        val initialLeft = main.paddingLeft
        val initialTop = main.paddingTop
        val initialRight = main.paddingRight
        val initialBottom = main.paddingBottom
        main.setOnApplyWindowInsetsListener { _, insets ->
            val systemBars = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                insets.getInsets(WindowInsets.Type.systemBars())
            } else {
                android.graphics.Insets.of(
                    insets.systemWindowInsetLeft,
                    insets.systemWindowInsetTop,
                    insets.systemWindowInsetRight,
                    insets.systemWindowInsetBottom
                )
            }
            main.setPadding(
                initialLeft + systemBars.left,
                initialTop + systemBars.top,
                initialRight + systemBars.right,
                initialBottom + systemBars.bottom
            )
            insets
        }
        main.requestApplyInsets()
    }

    private fun openRootRequestDialog() {
        val container = FrameLayout(this).apply {
            val padding = (16 * resources.displayMetrics.density).toInt()
            setPadding(padding, (8 * resources.displayMetrics.density).toInt(), padding, 0)
        }
        val input = EditText(this).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            setBackgroundResource(R.drawable.bg_input)
            val pad = (12 * resources.displayMetrics.density).toInt()
            setPadding(pad, pad, pad, pad)
            hint = getString(R.string.su_executable_label)
            val saved = getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
                .getString(SU_EXECUTABLE_KEY, DEFAULT_SU_EXECUTABLE)
                ?: DEFAULT_SU_EXECUTABLE
            setText(saved)
            setSelection(text?.length ?: 0)
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_URI
            isSingleLine = true
        }
        container.addView(input)

        val dialog = AlertDialog.Builder(this)
            .setTitle(R.string.ssaid_root_request_title)
            .setMessage(R.string.ssaid_root_request_message)
            .setView(container)
            .setNegativeButton(R.string.cancel, null)
            .setPositiveButton(R.string.request_root, null)
            .create()

        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val executable = input.text?.toString()?.trim().orEmpty()
                if (executable.isEmpty()) {
                    input.error = getString(R.string.su_executable_required)
                    return@setOnClickListener
                }
                getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
                    .edit()
                    .putString(SU_EXECUTABLE_KEY, executable)
                    .apply()
                dialog.dismiss()
                requestRoot(executable)
            }
        }
        dialog.show()
    }

    private fun requestRoot(suExecutable: String) {
        activeSuExecutable = null
        updateStatusDot(R.color.status_info)
        setOperationRunning(true, R.string.root_requesting)
        findViewById<TextView>(R.id.ssaidRootStatus).setText(R.string.root_requesting)
        backgroundExecutor.execute {
            try {
                val entries = rootRepository.readEntries(suExecutable)
                runOnUiThread {
                    activeSuExecutable = suExecutable
                    setOperationRunning(false)
                    updateStatusDot(R.color.status_active)
                    setLoadedEntries(entries)
                    showMessage(getString(R.string.root_granted, entries.size))
                }
            } catch (error: RootOperationException) {
                runOnUiThread {
                    setOperationRunning(false)
                    updateStatusDot(R.color.status_error)
                    findViewById<TextView>(R.id.ssaidRootStatus).text = error.message
                        ?: getString(R.string.root_failed)
                    showMessage(getString(R.string.root_failed))
                }
            }
        }
    }

    private fun loadSsaidEntries(suExecutable: String, showLoading: Boolean) {
        if (showLoading) {
            updateStatusDot(R.color.status_info)
            setOperationRunning(true, R.string.ssaid_loading)
            findViewById<TextView>(R.id.ssaidRootStatus).setText(R.string.ssaid_loading)
        }
        backgroundExecutor.execute {
            try {
                val entries = rootRepository.readEntries(suExecutable)
                runOnUiThread {
                    activeSuExecutable = suExecutable
                    setOperationRunning(false)
                    updateStatusDot(R.color.status_active)
                    setLoadedEntries(entries)
                }
            } catch (error: RootOperationException) {
                runOnUiThread {
                    setOperationRunning(false)
                    updateStatusDot(R.color.status_error)
                    findViewById<TextView>(R.id.ssaidRootStatus).text = error.message
                        ?: getString(R.string.root_failed)
                    showMessage(getString(R.string.root_failed))
                }
            }
        }
    }

    private fun setLoadedEntries(entries: List<SsaidEntry>) {
        loadedEntries = entries.sortedWith(
            compareBy<SsaidEntry> { applicationLabel(it.packageName).lowercase(Locale.getDefault()) }
                .thenBy { it.packageName }
        )
        findViewById<View>(R.id.searchContainer).visibility =
            if (loadedEntries.isNotEmpty()) View.VISIBLE else View.GONE
        findViewById<View>(R.id.filterChipsScroll).visibility =
            if (loadedEntries.isNotEmpty()) View.VISIBLE else View.GONE
        findViewById<Button>(R.id.manageSsaidButton).setText(R.string.refresh_ssaid)
        updateFilterChipsUi()
        applyFilterAndRender()
    }

    private fun applyFilterAndRender() {
        val container = findViewById<LinearLayout>(R.id.ssaidListContainer)
        val emptyState = findViewById<TextView>(R.id.ssaidEmptyState)
        val countBadge = findViewById<TextView>(R.id.ssaidCountBadge)
        container.removeAllViews()

        val filtered = loadedEntries.filter { entry ->
            val matchesFilter = when (currentFilterType) {
                AppFilter.ALL -> true
                AppFilter.USER -> !isSystemApp(entry.packageName)
                AppFilter.SYSTEM -> isSystemApp(entry.packageName)
                AppFilter.MODIFIED -> hasHistory(entry.packageName)
            }
            val matchesQuery = if (currentFilterQuery.isEmpty()) true else {
                applicationLabel(entry.packageName).contains(currentFilterQuery, ignoreCase = true) ||
                    entry.packageName.contains(currentFilterQuery, ignoreCase = true) ||
                    entry.value.contains(currentFilterQuery, ignoreCase = true)
            }
            matchesFilter && matchesQuery
        }

        if (loadedEntries.isNotEmpty()) {
            countBadge.visibility = View.VISIBLE
            countBadge.text = getString(R.string.apps_count, filtered.size)
            findViewById<TextView>(R.id.ssaidRootStatus).text = getString(R.string.status_authorized)
        }

        filtered.forEach { entry ->
            val row = LayoutInflater.from(this)
                .inflate(R.layout.item_ssaid, container, false)
            row.findViewById<TextView>(R.id.ssaidAppLabel).text = applicationLabel(entry.packageName)
            row.findViewById<TextView>(R.id.ssaidPackageName).text = entry.packageName
            row.findViewById<TextView>(R.id.ssaidEntryValue).apply {
                text = entry.value
                setOnClickListener {
                    getSystemService(ClipboardManager::class.java).setPrimaryClip(
                        ClipData.newPlainText(entry.packageName, entry.value)
                    )
                    showMessage(getString(R.string.copied, entry.packageName))
                }
            }
            row.findViewById<Button>(R.id.editSsaidButton).setOnClickListener {
                openEditDialog(entry)
            }
            row.findViewById<Button>(R.id.randomSsaidButton).setOnClickListener {
                openRandomDialog(entry)
            }
            row.findViewById<Button>(R.id.historySsaidButton).setOnClickListener {
                showHistory(entry)
            }
            row.findViewById<Button>(R.id.clearDataButton).setOnClickListener {
                confirmClearData(entry)
            }
            container.addView(row)
        }

        val hasLoaded = loadedEntries.isNotEmpty()
        val hasFiltered = filtered.isNotEmpty()
        if (!hasLoaded) {
            emptyState.visibility = View.VISIBLE
            emptyState.setText(R.string.ssaid_list_empty)
            container.visibility = View.GONE
        } else if (!hasFiltered) {
            emptyState.visibility = View.VISIBLE
            emptyState.setText(R.string.no_matching_apps)
            container.visibility = View.GONE
        } else {
            emptyState.visibility = View.GONE
            container.visibility = View.VISIBLE
        }
    }

    private fun updateStatusDot(colorRes: Int) {
        val dot = findViewById<View>(R.id.rootStatusDot)
        dot.backgroundTintList = ColorStateList.valueOf(getColor(colorRes))
    }

    private fun applicationLabel(packageName: String): String = try {
        val applicationInfo = packageManager.getApplicationInfo(packageName, PackageManager.GET_META_DATA)
        packageManager.getApplicationLabel(applicationInfo).toString()
    } catch (_: PackageManager.NameNotFoundException) {
        packageName
    }

    private fun openEditDialog(entry: SsaidEntry) {
        val container = FrameLayout(this).apply {
            val padding = (16 * resources.displayMetrics.density).toInt()
            setPadding(padding, (8 * resources.displayMetrics.density).toInt(), padding, 0)
        }
        val input = EditText(this).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            setBackgroundResource(R.drawable.bg_input)
            val pad = (12 * resources.displayMetrics.density).toInt()
            setPadding(pad, pad, pad, pad)
            hint = getString(R.string.new_ssaid_label)
            setText(entry.value)
            setSelection(text?.length ?: 0)
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
            isSingleLine = true
        }
        container.addView(input)

        val dialog = AlertDialog.Builder(this)
            .setTitle(getString(R.string.edit_ssaid_title, applicationLabel(entry.packageName)))
            .setMessage(entry.packageName)
            .setView(container)
            .setNegativeButton(R.string.cancel, null)
            .setPositiveButton(R.string.save, null)
            .create()
        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val value = input.text?.toString()?.trim().orEmpty()
                if (!isValidSsaid(value)) {
                    input.error = getString(R.string.invalid_ssaid)
                    return@setOnClickListener
                }
                dialog.dismiss()
                submitSsaidChange(entry, value)
            }
        }
        dialog.show()
    }

    private fun openRandomDialog(entry: SsaidEntry) {
        val newValue = randomSsaid()
        AlertDialog.Builder(this)
            .setTitle(R.string.random_ssaid_title)
            .setMessage(getString(R.string.random_ssaid_message, applicationLabel(entry.packageName), newValue))
            .setNegativeButton(R.string.cancel, null)
            .setPositiveButton(R.string.apply_change) { _, _ -> submitSsaidChange(entry, newValue) }
            .show()
    }

    private fun submitSsaidChange(entry: SsaidEntry, newValue: String) {
        val suExecutable = activeSuExecutable
        if (suExecutable == null) {
            openRootRequestDialog()
            return
        }
        if (!isValidSsaid(newValue)) {
            showMessage(getString(R.string.invalid_ssaid))
            return
        }
        if (entry.value.equals(newValue, ignoreCase = true)) {
            showMessage(getString(R.string.ssaid_unchanged))
            return
        }

        updateStatusDot(R.color.status_info)
        setOperationRunning(true, R.string.ssaid_saving)
        findViewById<TextView>(R.id.ssaidRootStatus).setText(R.string.ssaid_saving)
        backgroundExecutor.execute {
            try {
                val result = rootRepository.updateEntry(suExecutable, entry.packageName, newValue)
                historyStore.append(
                    entry.packageName,
                    SsaidHistoryRecord(result.previousValue, newValue, System.currentTimeMillis())
                )
                runOnUiThread {
                    setOperationRunning(false)
                    updateStatusDot(R.color.status_active)
                    setLoadedEntries(result.entries)
                    findViewById<TextView>(R.id.currentSsaidValue).text = currentSsaid()
                    showMessage(
                        getString(
                            R.string.ssaid_changed,
                            applicationLabel(entry.packageName),
                            newValue
                        )
                    )
                    showSsaidRestartNotice()
                }
            } catch (error: RootOperationException) {
                runOnUiThread {
                    setOperationRunning(false)
                    updateStatusDot(R.color.status_error)
                    findViewById<TextView>(R.id.ssaidRootStatus).text = error.message
                        ?: getString(R.string.ssaid_change_failed)
                    showMessage(getString(R.string.ssaid_change_failed))
                }
            }
        }
    }

    private fun showHistory(entry: SsaidEntry) {
        val records = historyStore.records(entry.packageName)
        if (records.isEmpty()) {
            showMessage(getString(R.string.ssaid_history_empty))
            return
        }

        val padding = (16 * resources.displayMetrics.density).toInt()
        val historyContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(padding, (8 * resources.displayMetrics.density).toInt(), padding, 0)
        }
        val scrollView = ScrollView(this).apply {
            addView(historyContainer)
        }

        lateinit var historyDialog: AlertDialog
        records.asReversed().forEach { record ->
            val item = LayoutInflater.from(this)
                .inflate(R.layout.item_ssaid_history, historyContainer, false)
            item.findViewById<TextView>(R.id.historyTime).text = formatTime(record.timestamp)
            item.findViewById<TextView>(R.id.historyValues).text = getString(
                R.string.history_values,
                record.oldValue,
                record.newValue
            )
            item.findViewById<Button>(R.id.restoreHistoryButton).setOnClickListener {
                historyDialog.dismiss()
                confirmRestore(entry, record)
            }
            historyContainer.addView(item)
        }

        historyDialog = AlertDialog.Builder(this)
            .setTitle(getString(R.string.history_title, applicationLabel(entry.packageName)))
            .setView(scrollView)
            .setPositiveButton(R.string.close, null)
            .create()
        historyDialog.show()
    }

    private fun confirmRestore(entry: SsaidEntry, record: SsaidHistoryRecord) {
        AlertDialog.Builder(this)
            .setTitle(R.string.restore_ssaid_title)
            .setMessage(getString(R.string.restore_ssaid_message, record.oldValue))
            .setNegativeButton(R.string.cancel, null)
            .setPositiveButton(R.string.restore) { _, _ -> submitSsaidChange(entry, record.oldValue) }
            .show()
    }

    private fun setOperationRunning(running: Boolean, messageRes: Int = R.string.refresh_ssaid) {
        if (!operationRunning.compareAndSet(!running, running)) return
        findViewById<Button>(R.id.manageSsaidButton).apply {
            isEnabled = !running
            if (running) setText(messageRes) else setText(R.string.refresh_ssaid)
        }
        findViewById<ImageButton>(R.id.refreshButton).isEnabled = !running
    }

    private fun copyIdentifier(labelRes: Int, valueViewId: Int) {
        val value = findViewById<TextView>(valueViewId).text.toString()
        if (value == getString(R.string.unavailable)) {
            showMessage(getString(R.string.nothing_to_copy))
            return
        }
        getSystemService(ClipboardManager::class.java).setPrimaryClip(
            ClipData.newPlainText(getString(labelRes), value)
        )
        showMessage(getString(R.string.copied, getString(labelRes)))
    }

    private fun showMessage(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun showSsaidRestartNotice() {
        AlertDialog.Builder(this)
            .setTitle(R.string.ssaid_change_notice_title)
            .setMessage(R.string.ssaid_change_notice_message)
            .setNegativeButton(R.string.got_it, null)
            .setPositiveButton(R.string.reboot_now) { _, _ -> openRebootMenu() }
            .show()
    }

    private fun openRebootMenu() {
        val suExecutable = activeSuExecutable
        if (suExecutable == null) {
            openRootRequestDialog()
            return
        }
        val options = arrayOf(
            getString(R.string.reboot_soft),
            getString(R.string.reboot_full)
        )
        AlertDialog.Builder(this)
            .setTitle(R.string.reboot_menu_title)
            .setItems(options) { _, which ->
                val mode = if (which == 0) RebootMode.SOFT_REBOOT else RebootMode.FULL_REBOOT
                executeReboot(mode)
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun executeReboot(mode: RebootMode) {
        val suExecutable = activeSuExecutable ?: return
        showMessage(getString(R.string.rebooting))
        backgroundExecutor.execute {
            try {
                rootRepository.reboot(suExecutable, mode)
            } catch (error: RootOperationException) {
                runOnUiThread {
                    showMessage(error.message ?: getString(R.string.root_failed))
                }
            }
        }
    }

    private fun confirmClearData(entry: SsaidEntry) {
        val suExecutable = activeSuExecutable
        if (suExecutable == null) {
            openRootRequestDialog()
            return
        }
        val appLabel = applicationLabel(entry.packageName)
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.clear_data_title, appLabel))
            .setMessage(getString(R.string.clear_data_message, appLabel))
            .setNegativeButton(R.string.cancel, null)
            .setPositiveButton(R.string.clear_data) { _, _ ->
                backgroundExecutor.execute {
                    try {
                        rootRepository.clearAppData(suExecutable, entry.packageName)
                        runOnUiThread {
                            showMessage(getString(R.string.clear_data_success, appLabel))
                        }
                    } catch (error: RootOperationException) {
                        runOnUiThread {
                            showMessage(error.message ?: getString(R.string.clear_data_failed, appLabel))
                        }
                    }
                }
            }
            .show()
    }

    private fun formatTime(timestamp: Long): String =
        SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(timestamp))

    private fun isValidSsaid(value: String): Boolean = value.matches(Regex("[0-9a-fA-F]{16}"))

    private fun randomSsaid(): String =
        UUID.randomUUID().toString().replace("-", "").take(16).lowercase(Locale.US)

    private companion object {
        const val PREFERENCES_NAME = "device_identifiers"
        const val SU_EXECUTABLE_KEY = "su_executable"
        const val DEFAULT_SU_EXECUTABLE = "su"
    }
}
