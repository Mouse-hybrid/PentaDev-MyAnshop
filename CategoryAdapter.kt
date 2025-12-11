package com.example.petshop

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FirebaseFirestore

class CategoryAdapter(private val categoryList: List<Category>) : RecyclerView.Adapter<CategoryAdapter.CategoryViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoryViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.category_item, parent, false)
        return CategoryViewHolder(view)
    }

    override fun onBindViewHolder(holder: CategoryViewHolder, position: Int) {
        val category = categoryList[position]
        holder.bind(category)
    }

    override fun getItemCount(): Int = categoryList.size

    class CategoryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val categoryName: TextView = itemView.findViewById(R.id.tv_category_name)
        private val deleteButton: ImageView = itemView.findViewById(R.id.btn_delete_category)

        fun bind(category: Category) {
            categoryName.text = category.name

            deleteButton.setOnClickListener {
                showDeleteConfirmationDialog(itemView.context, category)
            }
        }

        private fun showDeleteConfirmationDialog(context: Context, category: Category) {
            AlertDialog.Builder(context)
                .setTitle("Delete Category")
                .setMessage("Are you sure you want to delete '${category.name}'? This action cannot be undone.")
                .setPositiveButton("Yes") { _, _ ->
                    deleteCategoryFromFirestore(context, category.id)
                }
                .setNegativeButton("No", null)
                .show()
        }

        private fun deleteCategoryFromFirestore(context: Context, categoryId: String) {
            if (categoryId.isEmpty()) {
                Toast.makeText(context, "Error: Category ID is missing.", Toast.LENGTH_SHORT).show()
                return
            }
            val db = FirebaseFirestore.getInstance()
            db.collection("categories").document(categoryId)
                .delete()
                .addOnSuccessListener {
                    Toast.makeText(context, "Category deleted successfully", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(context, "Error deleting category: ${e.message}", Toast.LENGTH_LONG).show()
                }
        }
    }
}