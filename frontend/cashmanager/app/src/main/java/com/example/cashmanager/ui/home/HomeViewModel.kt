package com.example.cashmanager.ui.home

import android.util.Log
import android.widget.Toast
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.cashmanager.data.api.ApiService
import com.example.cashmanager.data.model.Account
import com.example.cashmanager.data.model.Basket
import com.example.cashmanager.data.model.Product
import com.google.gson.JsonObject
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class HomeViewModel : ViewModel() {

    val currentUser = MutableLiveData<Account>()
    val product = MutableLiveData<Product>()
    val scanStatusError = MutableLiveData<Boolean>()
    val products = MutableLiveData<List<Product>>().apply { value = ArrayList() }
    val totalBasket = MutableLiveData<Float>()
    val baskets = MutableLiveData<List<Basket>>().apply { value = ArrayList() }
    var currentBasketId : Int = 0

    private val apiService = ApiService()

    fun initUser() {
        apiService.user.read()
            .enqueue(object : Callback<Account> {
                override fun onFailure(call: Call<Account>, t: Throwable) {

                }

                override fun onResponse(call: Call<Account>, response: Response<Account>) {
                    if (response.isSuccessful)
                        currentUser.apply { value = response.body() }
                }
            })
    }

    fun updateUser(email: String, username: String, points : Int,  validToast: Toast, invalidToast: Toast) {
        val updatedAccount: Account = Account(email, username, points)
        apiService.user.update(updatedAccount)
            .enqueue(object : Callback<Account> {
                override fun onFailure(call: Call<Account>, t: Throwable) {
                    invalidToast.show()
                }

                override fun onResponse(call: Call<Account>, response: Response<Account>) {
                    if (response.isSuccessful) {
                        validToast.show()
                        currentUser.apply { value = response.body() }
                    } else {
                        invalidToast.show()
                    }
                }

            })
    }

    fun getProductById(product_id: Int) {
        apiService.product.getProductById(product_id)
            .enqueue(object : Callback<Product> {
                override fun onFailure(call: Call<Product>, t: Throwable) {
                    Log.e("Error on RESPONSE = ", t.localizedMessage)
                    scanStatusError.apply { value = true }
                }

                override fun onResponse(call: Call<Product>, response: Response<Product>) {
                    if (response.isSuccessful) {
                        Log.i("response = ", response.body().toString())
                        val productResponse: Product = response.body()!!
                        productResponse.quantity = 1
                        product.apply { value = productResponse }
                    }
                }
            })
    }

    fun getUserBaskets() {
        apiService.basket.getAllBaskets()
            .enqueue(object : Callback<List<Basket>> {
                override fun onFailure(call: Call<List<Basket>>, t: Throwable) {
                    Log.e("Error : ", t.localizedMessage)
                }

                override fun onResponse(call: Call<List<Basket>>, response: Response<List<Basket>>) {
                    Log.i("in response", response.body().toString())
                    if (response.isSuccessful) {
                        Log.i("Response OK !!", response.body().toString())
                        baskets.apply { value = response.body() }
                        Log.i("baskets = ", baskets.value.toString())
                    }
                }
            })
    }

    fun addProductToBasket() {
        var list: List<Product>? = products.value
        if (list != null) {
            list = list.toMutableList()
            val founded = list.find { p -> product.value!!.id == p.id }
            if (founded != null) {
                addQuantity(list.indexOf(founded))
            } else {
                list.add(product.value!!)
            }
            products.apply {
                value = list
            }
            product.apply { value = null }
        }
    }

    fun addQuantity(productIndex: Int) {
        val product = products.value?.get(productIndex)
        if (product!!.quantity < product!!.stockQuantity) {
            product?.apply { price += (price / quantity) }
            product?.apply { cashback += (cashback / quantity) }
            product?.apply { quantity += 1 }

            val productsTmp = products.value?.toMutableList()
            if (product != null) {
                productsTmp?.set(productIndex, product)
            }
            products.apply { value = productsTmp }
        }
    }

    fun removeQuantity(productIndex: Int) {
        var product = products.value?.get(productIndex)
        if (product?.quantity!! > 1) {
            product?.apply { price -= (price / quantity) }
            product?.apply { cashback -= (cashback / quantity) }
            product?.apply { quantity -= 1 }

            val productsTmp = products.value?.toMutableList()
            if (product != null) {
                productsTmp?.set(productIndex, product)
            }
            products.apply { value = productsTmp }
        }
    }

    fun removeProductFromBasket(productIndex: Int) {
        var productsTmp = products.value?.toMutableList()
        productsTmp?.removeAt(productIndex)
        products.apply { value = productsTmp }
    }

    fun validateBasket(status : String, validToast: Toast, invalidToast: Toast) {
        Log.i("update list", products.toString());
        val productsTmp: MutableList<Product>? = products.value?.toMutableList()
        productsTmp?.clear()
        updateBasket(status, validToast, invalidToast)
        products.apply { value = productsTmp }
    }

    fun createBasket(validToast: Toast, invalidToast: Toast) {
        val body = JSONObject()
        val products_tmp = JSONObject()
        for (p in products.value!!) {
            products_tmp.put(p.id.toString(), p.quantity.toString())
        }
        body.put("products", products_tmp)
        val jsonObjectString = body.toString()
        val requestBody = jsonObjectString.toRequestBody("application/json".toMediaTypeOrNull())
        apiService.basket.createBasket(requestBody)
            .enqueue(object : Callback<Basket> {
                override fun onFailure(call: Call<Basket>, t: Throwable) {
                    Log.e("Error on RESPONSE = ", t.localizedMessage)
                    invalidToast.show()
                }

                override fun onResponse(call: Call<Basket>, response: Response<Basket>) {
                    if (response.isSuccessful) {
                        currentBasketId = response.body()!!.id
                        validToast.show()
                    }
                }
            })
    }

    private fun updateBasket(status: String, validToast: Toast, invalidToast: Toast) {
        val body = JSONObject()
        body.put("status", status)
        val jsonObjectString = body.toString()
        val requestBody = jsonObjectString.toRequestBody("application/json".toMediaTypeOrNull())
        apiService.basket.updateBasket(currentBasketId, requestBody)
            .enqueue(object : Callback<Basket> {
                override fun onFailure(call: Call<Basket>, t: Throwable) {
                    Log.e("Error on RESPONSE = ", t.localizedMessage)
                    invalidToast.show()
                }

                override fun onResponse(call: Call<Basket>, response: Response<Basket>) {
                    if (response.isSuccessful) {
                        initUser()
                        getUserBaskets()
                        validToast.show()
                    }
                }
            })
    }

    fun paymentResult(paymentSuccess: Boolean, validToast: Toast?, invalidToast: Toast?) {
        if (paymentSuccess) {
            validToast?.show()
        } else {
            invalidToast?.show()
        }

    }

}