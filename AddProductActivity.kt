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
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import java.util.UUID

class AddProductActivity : AppCompatActivity() {

    private lateinit var db: FirebaseFirestore
    private lateinit var storage: FirebaseStorage
    private var imageUri: Uri? = null
    private lateinit var productImageView: ImageView
    private lateinit var categorySpinner: Spinner
    private val categoryList = mutableListOf<String>()

    companion object {
        private const val PICK_IMAGE_REQUEST = 1
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_product)

        db = FirebaseFirestore.getInstance()
        storage = FirebaseStorage.getInstance()

        productImageView = findViewById(R.id.img_product_preview)
        categorySpinner = findViewById(R.id.spinner_category)
        val btnSelectImage: Button = findViewById(R.id.btn_select_image)
        val edtProductName: TextInputEditText = findViewById(R.id.edtProductName)
        val edtProductPrice: TextInputEditText = findViewById(R.id.edtProductPrice)
        val edtProductDescription: TextInputEditText = findViewById(R.id.edtProductDescription)
        val btnAddProduct: Button = findViewById(R.id.btnAddProduct)

        setupCategorySpinner()

        btnSelectImage.setOnClickListener {
            openFileChooser()
        }

        btnAddProduct.setOnClickListener {
            uploadImageAndAddProduct(
                edtProductName.text.toString().trim(),
                edtProductPrice.text.toString().trim(),
                edtProductDescription.text.toString().trim()
            )
        }
    }

    private fun setupCategorySpinner() {
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, categoryList)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        categorySpinner.adapter = adapter

        db.collection("categories").orderBy("name").get()
            .addOnSuccessListener {
                categoryList.clear()
                for (document in it) {
                    categoryList.add(document.getString("name") ?: "")
                }
                adapter.notifyDataSetChanged()
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

    private fun uploadImageAndAddProduct(name: String, priceStr: String, description: String) {
        if (name.isEmpty() || priceStr.isEmpty()) {
            Toast.makeText(this, "Please fill name and price", Toast.LENGTH_SHORT).show()
            return
        }

        if (categorySpinner.selectedItem == null) {
            Toast.makeText(this, "Please select a category", Toast.LENGTH_SHORT).show()
            return
        }

        if (imageUri == null) {
            Toast.makeText(this, "Please select an image", Toast.LENGTH_SHORT).show()
            return
        }

        val price = priceStr.toDoubleOrNull()
        if (price == null) {
            Toast.makeText(this, "Invalid price format", Toast.LENGTH_SHORT).show()
            return
        }

        val fileName = UUID.randomUUID().toString()
        val storageRef = storage.reference.child("products/$fileName")

        storageRef.putFile(imageUri!!)
            .addOnSuccessListener { 
                storageRef.downloadUrl.addOnSuccessListener { uri ->
                    addProductToFirestore(name, price, description, uri.toString())
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Image upload failed: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun addProductToFirestore(name: String, price: Double, description: String, imageUrl: String) {
        val category = categorySpinner.selectedItem.toString()
        val product = hashMapOf(
            "name" to name,
            "price" to price,
            "description" to description,
            "imageUrl" to imageUrl,
            "category" to category
        )

        db.collection("products")
            .add(product)
            .addOnSuccessListener { 
                Toast.makeText(this, "Product added successfully!", Toast.LENGTH_SHORT).show()
                finish()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error adding product: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }
}