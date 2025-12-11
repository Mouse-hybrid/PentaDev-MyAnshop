package com.example.petshop

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.firebase.firestore.FirebaseFirestore

class ProductAdapter(private val productList: List<Product>, private val isAdmin: Boolean) : RecyclerView.Adapter<ProductAdapter.ProductViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.product_item, parent, false)
        return ProductViewHolder(view)
    }

    override fun onBindViewHolder(holder: ProductViewHolder, position: Int) {
        val product = productList[position]
        holder.bind(product, isAdmin)
    }

    override fun getItemCount(): Int = productList.size

    class ProductViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val productName: TextView = itemView.findViewById(R.id.tvProductName)
        private val productPrice: TextView = itemView.findViewById(R.id.tvProductPrice)
        private val productCategory: TextView = itemView.findViewById(R.id.tv_product_category)
        private val productImage: ImageView = itemView.findViewById(R.id.imgProduct)
        private val adminActionsLayout: LinearLayout = itemView.findViewById(R.id.admin_actions_layout)
        private val editButton: ImageView = itemView.findViewById(R.id.btn_edit_product)
        private val deleteButton: ImageView = itemView.findViewById(R.id.btn_delete_product)

        fun bind(product: Product, isAdmin: Boolean) {
            productName.text = product.name
            productPrice.text = "$${String.format("%.2f", product.price)}"
            productCategory.text = product.category

            productCategory.visibility = if (product.category.isNotEmpty()) View.VISIBLE else View.GONE

            // Clear previous image
            productImage.setImageDrawable(null)

            // Use Glide to load the image if the URL is not empty
            if (product.imageUrl.isNotEmpty()) {
                Glide.with(itemView.context)
                    .load(product.imageUrl)
                    .placeholder(R.drawable.ic_launcher_background) // Image shown while loading
                    .error(R.drawable.ic_launcher_foreground) // Image shown on error
                    .centerCrop()
                    .into(productImage)
            } else {
                // Set a default placeholder if no image URL is available
                productImage.setImageResource(R.drawable.ic_launcher_background)
            }

            if (isAdmin) {
                adminActionsLayout.visibility = View.VISIBLE

                deleteButton.setOnClickListener {
                    showDeleteConfirmationDialog(itemView.context, product)
                }

                editButton.setOnClickListener {
                    val context = itemView.context
                    val intent = Intent(context, EditProductActivity::class.java).apply {
                        putExtra("PRODUCT_EXTRA", product)
                    }
                    context.startActivity(intent)
                }
                
                itemView.setOnClickListener(null)

            } else {
                adminActionsLayout.visibility = View.GONE
                itemView.setOnClickListener {
                    val context = itemView.context
                    val intent = Intent(context, ProductDetailActivity::class.java).apply {
                        putExtra("PRODUCT_ID", product.id)
                        putExtra("PRODUCT_NAME", product.name)
                        putExtra("PRODUCT_PRICE", product.price)
                        putExtra("PRODUCT_IMAGE_URL", product.imageUrl)
                        putExtra("PRODUCT_DESCRIPTION", product.description)
                    }
                    context.startActivity(intent)
                }
            }
        }

        private fun showDeleteConfirmationDialog(context: Context, product: Product) {
            AlertDialog.Builder(context)
                .setTitle("Delete Product")
                .setMessage("Are you sure you want to delete '${product.name}'?")
                .setPositiveButton("Yes") { _, _ ->
                    deleteProductFromFirestore(context, product.id)
                }
                .setNegativeButton("No", null)
                .show()
        }

        private fun deleteProductFromFirestore(context: Context, productId: String) {
            if (productId.isEmpty()) {
                Toast.makeText(context, "Error: Product ID is missing.", Toast.LENGTH_SHORT).show()
                return
            }
            val db = FirebaseFirestore.getInstance()
            db.collection("products").document(productId)
                .delete()
                .addOnSuccessListener {
                    Toast.makeText(context, "Product deleted successfully", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(context, "Error deleting product: ${e.message}", Toast.LENGTH_LONG).show()
                }
        }
    }
}