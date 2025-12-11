package com.example.petshop

import android.app.Activity
import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ImageView
import android.widget.Spinner
import android.widget.Toast
import com.bumptech.glide.Glide
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import java.util.UUID

class EditProductActivity : AppCompatActivity() {

    private lateinit var db: FirebaseFirestore
    private lateinit var storage: FirebaseStorage
    private var imageUri: Uri? = null
    private var currentProduct: Product? = null
    
    // Declare UI components at class level
    private lateinit var productImageView: ImageView
    private lateinit var categorySpinner: Spinner
    private lateinit var edtProductName: TextInputEditText
    private lateinit var edtProductPrice: TextInputEditText
    private lateinit var edtProductDescription: TextInputEditText

    private val categoryList = mutableListOf<String>()
    private lateinit var adapter: ArrayAdapter<String>

    companion object {
        private const val PICK_IMAGE_REQUEST = 1
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_product)

        db = FirebaseFirestore.getInstance()
        storage = FirebaseStorage.getInstance()

        // Initialize UI components
        productImageView = findViewById(R.id.img_product_preview)
        categorySpinner = findViewById(R.id.spinner_category)
        edtProductName = findViewById(R.id.edtProductName)
        edtProductPrice = findViewById(R.id.edtProductPrice)
        edtProductDescription = findViewById(R.id.edtProductDescription)
        val btnSelectImage: Button = findViewById(R.id.btn_select_image)
        val btnUpdateProduct: Button = findViewById(R.id.btnUpdateProduct)

        setupCategorySpinner()

        currentProduct = intent.getParcelableExtra("PRODUCT_EXTRA")
        if (currentProduct == null) {
            Toast.makeText(this, "Error: Product not found.", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        populateProductData()

        btnSelectImage.setOnClickListener {
            openFileChooser()
        }

        btnUpdateProduct.setOnClickListener {
            updateProduct()
        }
    }

    private fun setupCategorySpinner() {
        adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, categoryList)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        categorySpinner.adapter = adapter

        db.collection("categories").orderBy("name").get()
            .addOnSuccessListener {
                categoryList.clear()
                for (document in it) {
                    categoryList.add(document.getString("name") ?: "")
                }
                adapter.notifyDataSetChanged()
                currentProduct?.let { product ->
                    val categoryPosition = adapter.getPosition(product.category)
                    if (categoryPosition >= 0) {
                        categorySpinner.setSelection(categoryPosition)
                    }
                }
            }
    }

    private fun populateProductData() {
        currentProduct?.let { product ->
            edtProductName.setText(product.name)
            edtProductPrice.setText(product.price.toString())
            edtProductDescription.setText(product.description)

            if (product.imageUrl.isNotEmpty()) {
                Glide.with(this).load(product.imageUrl).into(productImageView)
            }
        }
    }

    private fun openFileChooser() {
        val intent = Intent().apply {
            type = "image/*"
            action = Intent.ACTION_GET_CONTENT
        }
        startActivityForResult(intent, PICK_IMAGE_REQUEST)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK && data != null && data.data != null) {
            imageUri = data.data
            productImageView.setImageURI(imageUri)
        }
    }

    private fun updateProduct() {
        val name = edtProductName.text.toString().trim()
        val priceStr = edtProductPrice.text.toString().trim()
        val description = edtProductDescription.text.toString().trim()
        val category = categorySpinner.selectedItem.toString()

        if (name.isEmpty() || priceStr.isEmpty()) {
            Toast.makeText(this, "Please fill name and price", Toast.LENGTH_SHORT).show()
            return
        }

        val price = priceStr.toDoubleOrNull()
        if (price == null) {
            Toast.makeText(this, "Invalid price format", Toast.LENGTH_SHORT).show()
            return
        }

        if (imageUri != null) {
            uploadImageAndUpdateProduct(name, price, category, description)
        } else {
            updateProductInFirestore(currentProduct!!.id, name, price, category, description, currentProduct!!.imageUrl)
        }
    }

    private fun uploadImageAndUpdateProduct(name: String, price: Double, category: String, description: String) {
        val fileName = UUID.randomUUID().toString()
        val storageRef = storage.reference.child("products/$fileName")

        storageRef.putFile(imageUri!!)
            .addOnSuccessListener { 
                storageRef.downloadUrl.addOnSuccessListener { uri ->
                    updateProductInFirestore(currentProduct!!.id, name, price, category, description, uri.toString())
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Image upload failed: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun updateProductInFirestore(productId: String, name: String, price: Double, category: String, description: String, imageUrl: String) {
        val updatedProduct = mapOf(
            "name" to name,
            "price" to price,
            "category" to category,
            "description" to description,
            "imageUrl" to imageUrl
        )

        db.collection("products").document(productId)
            .update(updatedProduct)
            .addOnSuccessListener { 
                Toast.makeText(this, "Product updated successfully!", Toast.LENGTH_SHORT).show()
                finish()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error updating product: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }
}