package com.example.petshop

import android.os.Bundle
import android.text.InputType
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.firestore.FirebaseFirestore

class AdminCategoriesFragment : Fragment() {

    private lateinit var db: FirebaseFirestore
    private lateinit var adapter: CategoryAdapter
    private val categoryList = mutableListOf<Category>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_admin_categories, container, false)

        db = FirebaseFirestore.getInstance()

        val recyclerView: RecyclerView = view.findViewById(R.id.recycler_view_categories)
        val fab: FloatingActionButton = view.findViewById(R.id.fab_add_category)

        // Setup RecyclerView
        recyclerView.layoutManager = LinearLayoutManager(context)
        adapter = CategoryAdapter(categoryList)
        recyclerView.adapter = adapter

        fetchCategories()

        fab.setOnClickListener {
            showAddCategoryDialog()
        }

        return view
    }

    private fun fetchCategories() {
        db.collection("categories")
            .orderBy("name")
            .addSnapshotListener { snapshots, e ->
                if (e != null) {
                    Log.w("AdminCategories", "Listen failed.", e)
                    return@addSnapshotListener
                }

                categoryList.clear()
                for (doc in snapshots!!) {
                    val category = doc.toObject(Category::class.java)
                    categoryList.add(category)
                }
                adapter.notifyDataSetChanged()
            }
    }

    private fun showAddCategoryDialog() {
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("Add New Category")

        val input = EditText(requireContext()).apply {
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_WORDS
            hint = "Category Name"
        }
        builder.setView(input)

        builder.setPositiveButton("Add") { _, _ ->
            val categoryName = input.text.toString().trim()
            if (categoryName.isNotEmpty()) {
                addCategoryToFirestore(categoryName)
            } else {
                Toast.makeText(context, "Category name cannot be empty", Toast.LENGTH_SHORT).show()
            }
        }
        builder.setNegativeButton("Cancel", null)

        builder.show()
    }

    private fun addCategoryToFirestore(categoryName: String) {
        val category = hashMapOf("name" to categoryName)
        db.collection("categories")
            .add(category)
            .addOnSuccessListener {
                Toast.makeText(context, "Category added successfully", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(context, "Error adding category: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }
}