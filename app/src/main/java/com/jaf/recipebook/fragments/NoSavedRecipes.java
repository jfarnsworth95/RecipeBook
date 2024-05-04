package com.jaf.recipebook.fragments;

import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.jaf.recipebook.R;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link NoSavedRecipes#newInstance} factory method to
 * create an instance of this fragment.
 */
public class NoSavedRecipes extends Fragment {

    public NoSavedRecipes() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     * @return A new instance of fragment NoSavedRecipes.
     */
    // TODO: Rename and change types and number of parameters
    public static NoSavedRecipes newInstance() {
        NoSavedRecipes fragment = new NoSavedRecipes();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_no_saved_recipes, container, false);
    }
}