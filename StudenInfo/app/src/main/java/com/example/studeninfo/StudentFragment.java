package com.example.studeninfo;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

/**
 * StudentFragment demonstrates Fragment Context.
 *
 * A Fragment does NOT have its own Context like Activity does.
 * Instead it BORROWS context from its HOST Activity.
 *
 * Methods to get context in Fragment:
 * 1. getContext()       — returns host Activity's context (can be null before onAttach)
 * 2. requireContext()   — same but throws exception if null (safer, crashes early)
 * 3. getActivity()      — returns the host Activity itself
 */
public class StudentFragment extends Fragment {

    private static final String TAG = "StudentFragment";

    /**
     * onAttach() is called when the Fragment is attached to its host Activity.
     * THIS is the earliest point where Fragment Context is available.
     *
     * @param context — this IS the host Activity's context
     */
    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        // Context is now valid — Fragment is attached to Activity
        Log.d(TAG, "onAttach() — Fragment Context available: "
                + context.getClass().getSimpleName());
    }

    /**
     * onCreateView() inflates the fragment's layout.
     * We use getLayoutInflater() which internally uses Fragment Context.
     */
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        // Inflate the fragment's layout using the provided LayoutInflater
        // The inflater already has the correct context attached
        View view = inflater.inflate(R.layout.fragment_student, container, false);
        return view;
    }

    /**
     * onViewCreated() is called after the view is ready.
     * This is the best place to interact with your UI elements.
     * getContext() is safely available here.
     */
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Find UI elements inside the fragment's view
        TextView tvFragmentInfo = view.findViewById(R.id.tvFragmentInfo);
        TextView tvContextType = view.findViewById(R.id.tvContextType);

        /*
         * FRAGMENT CONTEXT — using getContext()
         * WHY: We need context to access resources and Application data.
         *
         * requireContext() is the SAFER alternative to getContext()
         * because it throws a clear exception if context is null,
         * rather than silently crashing with a NullPointerException.
         */
        Context fragmentContext = requireContext();
        Log.d(TAG, "Fragment Context: " + fragmentContext.getClass().getSimpleName());

        // Access Application Context from Fragment
        MyApplication myApp = (MyApplication) fragmentContext.getApplicationContext();

        // Display info in fragment
        tvFragmentInfo.setText("Student: " + myApp.getStudentName()
                + "\nCourse: " + myApp.getStudentCourse());

        tvContextType.setText("Fragment Context Type:\n"
                + fragmentContext.getClass().getSimpleName());

        // Show Toast using Fragment Context
        Toast.makeText(requireContext(),
                "Fragment loaded using Fragment Context!",
                Toast.LENGTH_SHORT).show();
    }

    /**
     * onDetach() is called when Fragment is detached from Activity.
     * After this, getContext() returns null.
     * This is why you must NOT use context after this point.
     */
    @Override
    public void onDetach() {
        super.onDetach();
        Log.d(TAG, "onDetach() — Fragment Context is now NULL");
    }
}