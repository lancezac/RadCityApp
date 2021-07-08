package com.example.radcityapp;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Switch;
import android.widget.TextView;

import androidx.fragment.app.DialogFragment;

/**
 * Dialog for configuring fall detection
 */
public class FallDetectionDlg extends DialogFragment {
    private Activity mActivity;
    Switch mSwitch;
    TextView mTextView;

    /**
     * Interface to listen for dialog confirmation
     */
    public interface FallDetectionDlgListener{
        public void onDialogPositiveClick(FallDetectionDlg dlg);
    }

    FallDetectionDlgListener listener;

    /**
     * Register listener on attach
     * @param context
     */
    @Override
    public void onAttach(Context context){
        super.onAttach(context);
        try{
            listener = (FallDetectionDlgListener) context;
        } catch(ClassCastException e) {
            throw new ClassCastException(getActivity().toString());
        }
    }

    /**
     * Setup dialog
     * @param savedInstanceState
     * @return
     */
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState){
        mActivity = getActivity();

        AlertDialog.Builder b = new AlertDialog.Builder(mActivity);
        b.setTitle("Configure Fall Detection");
        LayoutInflater inflater = mActivity.getLayoutInflater();

        View view = inflater.inflate(R.layout.fall_dlg, null);
        b.setView(view);

        b.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {
                listener.onDialogPositiveClick(FallDetectionDlg.this);
                boolean check = mSwitch.isChecked();
            }
        });

        mSwitch = (Switch) view.findViewById(R.id.EnableDetection);
        mTextView = (TextView) view.findViewById(R.id.editTextPhone);

        final AlertDialog dlg = b.create();

        mSwitch.setChecked(getArguments().getBoolean("enabled"));
        mTextView.setText(getArguments().getString("phoneNum") );

        return dlg;
    }
}
