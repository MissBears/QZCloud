package com.smile.qzclould.ui.user.loign.fragment

import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import android.text.Editable
import android.text.TextWatcher
import androidx.navigation.Navigation
import com.smile.qielive.common.BaseFragment
import com.smile.qzclould.R
import com.smile.qzclould.common.App
import com.smile.qzclould.common.Constants
import com.smile.qzclould.manager.UserInfoManager
import com.smile.qzclould.ui.MainActivity
import com.smile.qzclould.ui.user.loign.viewmodel.UserViewModel
import com.smile.qzclould.utils.CommonUtils
import kotlinx.android.synthetic.main.fragment_regist_input_password.*

class RegistInputPasswordFragment : BaseFragment() {
    private var mPhoneNum: String? = null
    private var mCountryCode: String? = null
    private var mPhoneInfo: String? = null
    private var mCountDownTimer: CountDownTimer? = null
    private val mModel by lazy { ViewModelProviders.of(this).get(UserViewModel::class.java) }

    override fun getLayoutId(): Int {
        return R.layout.fragment_regist_input_password
    }

    override fun initView(savedInstanceState: Bundle?) {
        mCountDownTimer = object : CountDownTimer(120000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                tv_get_vcode.isEnabled = false
                tv_get_vcode.setTextColor(resources.getColor(R.color.color_gray_919191))
                tv_get_vcode.text = "${millisUntilFinished / 1000}秒后重试"
            }

            override fun onFinish() {
                tv_get_vcode.isEnabled = true
                tv_get_vcode.setTextColor(resources.getColor(R.color.color_black_404040))
                tv_get_vcode.text = getString(R.string.login_get_vcode)
            }
        }
        mCountDownTimer?.start()
    }

    override fun initData() {
        mPhoneNum = arguments?.getString("phone_num")
        mCountryCode = arguments?.getString("country_code")
        mPhoneInfo = arguments?.getString("phone_info")
    }

    override fun initListener() {
        tv_get_vcode.setOnClickListener {
            showLoading()
            mModel.sendRegisterMessage(mCountryCode!!, mPhoneNum!!)
        }

        btn_complete.setOnClickListener {
            showLoading()
            mModel.register(mPhoneInfo!!, et_vcode.text.toString(), mPhoneNum!!, CommonUtils.encodeMD5(et_pwd.text.toString()))
        }
        et_vcode.addTextChangedListener(object : TextWatcher {
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                btn_complete.isEnabled = et_vcode.text.toString().isNotEmpty() && et_pwd.text.toString().isNotEmpty()
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {

            }

            override fun afterTextChanged(s: Editable?) {

            }
        })
        et_pwd.addTextChangedListener(object : TextWatcher {
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                btn_complete.isEnabled = et_vcode.text.toString().isNotEmpty() && et_pwd.text.toString().isNotEmpty()
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {

            }

            override fun afterTextChanged(s: Editable?) {

            }
        })
        iv_close.setOnClickListener {
            Navigation.findNavController(it).navigateUp()
        }
    }

    override fun initViewModel() {
        mModel.verifyCodeResult.observe(this, Observer {
            stopLoading()
            showToast(Constants.TOAST_SUCCESS, getString(R.string.send_success))
            mCountDownTimer?.start()
        })

        mModel.loginResult.observe(this, Observer {
            stopLoading()
            showToast(Constants.TOAST_SUCCESS, App.instance.getString(R.string.register_success))
            UserInfoManager.get().saveUserInfo(it)
            val intent = Intent(mActivity, MainActivity::class.java)
            startActivity(intent)
            mActivity?.finish()
        })

        mModel.errorStatus.observe(this, Observer {
            stopLoading()
            showToast(Constants.TOAST_NORMAL, it?.errorMessage!!)
        })
    }

    override fun onDestroyView() {
        super.onDestroyView()
        mCountDownTimer?.cancel()
    }
}