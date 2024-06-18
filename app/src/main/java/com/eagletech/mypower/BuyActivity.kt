package com.eagletech.mypower

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import com.amazon.device.drm.LicensingService
import com.amazon.device.iap.PurchasingListener
import com.amazon.device.iap.PurchasingService
import com.amazon.device.iap.model.FulfillmentResult
import com.amazon.device.iap.model.ProductDataResponse
import com.amazon.device.iap.model.PurchaseResponse
import com.amazon.device.iap.model.PurchaseUpdatesResponse
import com.amazon.device.iap.model.UserDataResponse
import com.eagletech.mypower.data.ManagerData
import com.eagletech.mypower.databinding.ActivityBuyBinding

class BuyActivity : AppCompatActivity() {
    private lateinit var sBinding: ActivityBuyBinding
    private lateinit var myData: ManagerData
    private lateinit var currentUserId: String
    private lateinit var currentMarketplace: String


    companion object {
        const val power5 = "com.eagletech.mypower.power5"
        const val power10 = "com.eagletech.mypower.power10"
        const val power15 = "com.eagletech.mypower.power15"
        const val subPower = "com.eagletech.mypower.subpower"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sBinding = ActivityBuyBinding.inflate(layoutInflater)
        setContentView(sBinding.root)
        myData = ManagerData.getInstance(this)
        setupIAPOnCreate()
        setClickItems()

    }

    private fun setClickItems() {
        sBinding.power5.setOnClickListener {
            myData.addData(2)
            PurchasingService.purchase(power5)
        }
        sBinding.power10.setOnClickListener {
            PurchasingService.purchase(power10)
        }
        sBinding.power15.setOnClickListener {
            PurchasingService.purchase(power15)
        }
        sBinding.subPower.setOnClickListener {
            PurchasingService.purchase(subPower)
        }
        sBinding.finish.setOnClickListener { finish() }
        sBinding.back.setOnClickListener { finish() }
    }

    private fun setupIAPOnCreate() {
        val purchasingListener: PurchasingListener = object : PurchasingListener {
            override fun onUserDataResponse(response: UserDataResponse) {
                when (response.requestStatus!!) {
                    UserDataResponse.RequestStatus.SUCCESSFUL -> {
                        currentUserId = response.userData.userId
                        currentMarketplace = response.userData.marketplace
                        myData.userId(currentUserId)
                    }

                    UserDataResponse.RequestStatus.FAILED, UserDataResponse.RequestStatus.NOT_SUPPORTED -> Log.v(
                        "IAP SDK",
                        "loading failed"
                    )
                }
            }

            override fun onProductDataResponse(productDataResponse: ProductDataResponse) {
                when (productDataResponse.requestStatus) {
                    ProductDataResponse.RequestStatus.SUCCESSFUL -> {
                        val products = productDataResponse.productData
                        for (key in products.keys) {
                            val product = products[key]
                            Log.v(
                                "Product:", String.format(
                                    "Product: %s\n Type: %s\n SKU: %s\n Price: %s\n Description: %s\n",
                                    product!!.title,
                                    product.productType,
                                    product.sku,
                                    product.price,
                                    product.description
                                )
                            )
                        }
                        //get all unavailable SKUs
                        for (s in productDataResponse.unavailableSkus) {
                            Log.v("Unavailable SKU:$s", "Unavailable SKU:$s")
                        }
                    }

                    ProductDataResponse.RequestStatus.FAILED -> Log.v("FAILED", "FAILED")
                    else -> {}
                }
            }

            override fun onPurchaseResponse(purchaseResponse: PurchaseResponse) {
                when (purchaseResponse.requestStatus) {
                    PurchaseResponse.RequestStatus.SUCCESSFUL -> {

                        if (purchaseResponse.receipt.sku == power5) {
                            myData.addData(5)
                            showInfoDialog()
                        }
                        if (purchaseResponse.receipt.sku == power10) {
                            myData.addData(10)
                            showInfoDialog()
                        }
                        if (purchaseResponse.receipt.sku == power15) {
                            myData.addData(15)
                            showInfoDialog()
                        }
                        if (purchaseResponse.receipt.sku == subPower) {
                            myData.isPremium = true
                            showInfoDialog()
                        }
                        PurchasingService.notifyFulfillment(
                            purchaseResponse.receipt.receiptId, FulfillmentResult.FULFILLED
                        )

                        Log.v("FAILED", "FAILED")
                    }

                    PurchaseResponse.RequestStatus.FAILED -> {}
                    else -> {}
                }
            }

            override fun onPurchaseUpdatesResponse(response: PurchaseUpdatesResponse) {
                // Process receipts
                when (response.requestStatus) {
                    PurchaseUpdatesResponse.RequestStatus.SUCCESSFUL -> {
                        for (receipt in response.receipts) {
                            myData.isPremium = !receipt.isCanceled
                        }
                        if (response.hasMore()) {
                            PurchasingService.getPurchaseUpdates(false)
                        }

                    }

                    PurchaseUpdatesResponse.RequestStatus.FAILED -> Log.d("FAILED", "FAILED")
                    else -> {}
                }
            }
        }
        PurchasingService.registerListener(this, purchasingListener)
        Log.d(
            "DetailBuyAct", "Appstore SDK Mode: " + LicensingService.getAppstoreSDKMode()
        )
    }

    private fun showInfoDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dailog_info_buy, null)
        val messageTextView = dialogView.findViewById<TextView>(R.id.tvMessageBuy)
        val confirmButton = dialogView.findViewById<Button>(R.id.btnConfirmBuy)

        if (myData.isPremium == true) {
            messageTextView.text = "You have successfully registered"
        } else {
            messageTextView.text = "You have ${myData.getData()} uses"
        }

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setTitle("Info")
            .create()

        confirmButton.setOnClickListener {
            dialog.dismiss()
            finish()  // Navigates back to the previous activity
        }

        dialog.show()
    }


    override fun onResume() {
        super.onResume()
        PurchasingService.getUserData()
        val productSkus: MutableSet<String> = HashSet()
        productSkus.add(subPower)
        productSkus.add(power5)
        productSkus.add(power10)
        productSkus.add(power15)
        PurchasingService.getProductData(productSkus)
        PurchasingService.getPurchaseUpdates(false)
    }
}
