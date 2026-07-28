package com.homektv.tv.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.homektv.tv.R
import com.homektv.tv.databinding.ActivitySetupBinding
import com.homektv.tv.net.AppConfig
import com.homektv.tv.net.LanDiscovery
import com.homektv.tv.net.LanScanner
import kotlinx.coroutines.launch

/**
 * 首次配置页（详设§12.1 改造）：
 * 进入即自动扫描局域网中的点歌服务（HTTP 子网探测），扫到直接连；
 * 扫不到再展开手输兜底。已配置则直接进 MainActivity。
 */
class SetupActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySetupBinding
    private lateinit var config: AppConfig
    private lateinit var discovery: LanDiscovery
    private val scanner = LanScanner()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        config = AppConfig(this)
        discovery = LanDiscovery(this)

        binding = ActivitySetupBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnConnect.setOnClickListener { submitManual() }
        binding.btnRescan.setOnClickListener { startScan() }

        startScan()
    }

    /** 启动一次自动扫描；扫到即保存并进主界面，扫不到展开手输。 */
    private fun startScan() {
        showScanning()
        lifecycleScope.launch {
            val host = discovery.discover(
                savedHost = config.serverHost,
                onStage = { stage -> runOnUiThread { showStage(stage) } },
                onProgress = { scanned, total -> runOnUiThread {
                    binding.progressScan.max = total
                    binding.progressScan.progress = scanned
                    binding.txtScanStatus.text = getString(R.string.setup_scan_progress, scanned, total)
                } },
            )
            if (host != null) {
                binding.txtScanStatus.text = getString(R.string.setup_found, host)
                config.serverHost = host
                goMain()
            } else {
                showManualFallback()
            }
        }
    }

    private fun showStage(stage: LanDiscovery.Stage) {
        binding.progressScan.visibility = if (stage == LanDiscovery.Stage.SUBNET) View.VISIBLE else View.GONE
        binding.txtScanStatus.setText(when (stage) {
            LanDiscovery.Stage.SAVED -> R.string.setup_checking_saved
            LanDiscovery.Stage.MDNS -> R.string.setup_discovering_mdns
            LanDiscovery.Stage.UDP -> R.string.setup_discovering_udp
            LanDiscovery.Stage.SUBNET -> R.string.setup_scanning
        })
    }

    private fun showScanning() {
        binding.txtScanStatus.text = getString(R.string.setup_scanning)
        binding.progressScan.progress = 0
        binding.progressScan.visibility = View.VISIBLE
        binding.txtManualHint.visibility = View.GONE
        binding.inputHost.visibility = View.GONE
        binding.btnConnect.visibility = View.GONE
        binding.btnRescan.visibility = View.GONE
    }

    private fun showManualFallback() {
        binding.txtScanStatus.text = getString(R.string.setup_not_found)
        binding.progressScan.visibility = View.GONE
        binding.txtManualHint.visibility = View.VISIBLE
        binding.inputHost.visibility = View.VISIBLE
        binding.btnConnect.visibility = View.VISIBLE
        binding.btnRescan.visibility = View.VISIBLE
        binding.inputHost.requestFocus()
    }

    private fun submitManual() {
        val host = AppConfig.normalizeHost(binding.inputHost.text.toString())
        if (host == null) {
            Toast.makeText(this, R.string.setup_empty, Toast.LENGTH_SHORT).show()
            return
        }
        // 先探测 /api/health 确认是点歌服务端，校验通过才允许进入下一步
        binding.btnConnect.isEnabled = false
        binding.txtScanStatus.text = getString(R.string.setup_verifying, host)
        lifecycleScope.launch {
            val reachable = scanner.validate(host)
            if (reachable) {
                config.serverHost = host
                goMain()
            } else {
                binding.btnConnect.isEnabled = true
                binding.txtScanStatus.text = getString(R.string.setup_not_found)
                Toast.makeText(this@SetupActivity, R.string.setup_invalid, Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun goMain() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}
