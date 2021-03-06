/*
 * Copyright (C) 2014 Dominik Schürmann <dominik@dominikschuermann.de>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.sufficientlysecure.keychain.ui;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Messenger;
import android.support.v4.app.Fragment;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;

import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.ui.CreateKeyActivity.FragAction;
import org.sufficientlysecure.keychain.ui.dialog.AddEmailDialogFragment;
import org.sufficientlysecure.keychain.ui.dialog.SetPassphraseDialogFragment;
import org.sufficientlysecure.keychain.ui.util.Notify;
import org.sufficientlysecure.keychain.ui.widget.EmailEditText;

import java.util.ArrayList;
import java.util.List;

public class CreateKeyEmailFragment extends Fragment {

    CreateKeyActivity mCreateKeyActivity;
    EmailEditText mEmailEdit;
    RecyclerView mEmailsRecyclerView;
    View mBackButton;
    View mNextButton;

    ArrayList<EmailAdapter.ViewModel> mAdditionalEmailModels;

    EmailAdapter mEmailAdapter;

    /**
     * Creates new instance of this fragment
     */
    public static CreateKeyEmailFragment newInstance() {
        CreateKeyEmailFragment frag = new CreateKeyEmailFragment();

        Bundle args = new Bundle();
        frag.setArguments(args);

        return frag;
    }

    /**
     * Checks if text of given EditText is not empty. If it is empty an error is
     * set and the EditText gets the focus.
     *
     * @param context
     * @param editText
     * @return true if EditText is not empty
     */
    private static boolean isEditTextNotEmpty(Context context, EditText editText) {
        boolean output = true;
        if (editText.getText().length() == 0) {
            editText.setError(context.getString(R.string.create_key_empty));
            editText.requestFocus();
            output = false;
        } else {
            editText.setError(null);
        }

        return output;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.create_key_email_fragment, container, false);

        mEmailEdit = (EmailEditText) view.findViewById(R.id.create_key_email);
        mBackButton = view.findViewById(R.id.create_key_back_button);
        mNextButton = view.findViewById(R.id.create_key_next_button);
        mEmailsRecyclerView = (RecyclerView) view.findViewById(R.id.create_key_emails);

        // initial values
        mEmailEdit.setText(mCreateKeyActivity.mEmail);

        // focus empty edit fields
        if (mCreateKeyActivity.mEmail == null) {
            mEmailEdit.requestFocus();
        }
        mBackButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mCreateKeyActivity.loadFragment(null, FragAction.TO_LEFT);
            }
        });
        mNextButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                nextClicked();
            }
        });
        mEmailsRecyclerView.setHasFixedSize(true);
        mEmailsRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        mEmailsRecyclerView.setItemAnimator(new DefaultItemAnimator());

        // initial values
        if (mAdditionalEmailModels == null) {
            mAdditionalEmailModels = new ArrayList<>();
        }

        if (mEmailAdapter == null) {
            mEmailAdapter = new EmailAdapter(mAdditionalEmailModels, new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    addEmail();
                }
            });

            if (mCreateKeyActivity.mAdditionalEmails != null) {
                mEmailAdapter.addAll(mCreateKeyActivity.mAdditionalEmails);
            }
        }

        mEmailsRecyclerView.setAdapter(mEmailAdapter);

        return view;
    }

    private void addEmail() {
        Handler returnHandler = new Handler() {
            @Override
            public void handleMessage(Message message) {
                if (message.what == SetPassphraseDialogFragment.MESSAGE_OKAY) {
                    Bundle data = message.getData();

                    String email = data.getString(AddEmailDialogFragment.MESSAGE_DATA_EMAIL);

                    if (email.length() > 0 && mEmailEdit.getText().length() > 0 &&
                            email.equals(mEmailEdit.getText().toString())) {
                        Notify.create(getActivity(),
                                getString(R.string.create_key_email_already_exists_text),
                                Notify.LENGTH_LONG, Notify.Style.ERROR).show(CreateKeyEmailFragment.this);
                        return;
                    }
                    //check for duplicated emails inside the adapter
                    for (EmailAdapter.ViewModel model : mAdditionalEmailModels) {
                        if (email.equals(model.email)) {
                            Notify.create(getActivity(),
                                    getString(R.string.create_key_email_already_exists_text),
                                    Notify.LENGTH_LONG, Notify.Style.ERROR).show(CreateKeyEmailFragment.this);
                            return;
                        }
                    }

                    // add new user id
                    mEmailAdapter.add(email);
                }
            }
        };

        // Create a new Messenger for the communication back
        Messenger messenger = new Messenger(returnHandler);

        AddEmailDialogFragment addEmailDialog = AddEmailDialogFragment.newInstance(messenger);

        addEmailDialog.show(getActivity().getSupportFragmentManager(), "addEmailDialog");
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        mCreateKeyActivity = (CreateKeyActivity) getActivity();
    }

    private void nextClicked() {
        if (isEditTextNotEmpty(getActivity(), mEmailEdit)) {
            // save state
            mCreateKeyActivity.mEmail = mEmailEdit.getText().toString();
            mCreateKeyActivity.mAdditionalEmails = getAdditionalEmails();

            CreateKeyPassphraseFragment frag = CreateKeyPassphraseFragment.newInstance();
            mCreateKeyActivity.loadFragment(frag, FragAction.TO_RIGHT);
        }
    }

    private ArrayList<String> getAdditionalEmails() {
        ArrayList<String> emails = new ArrayList<>();
        for (EmailAdapter.ViewModel holder : mAdditionalEmailModels) {
            emails.add(holder.toString());
        }
        return emails;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        // save state in activity
        mCreateKeyActivity.mAdditionalEmails = getAdditionalEmails();
    }

    public static class EmailAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
        private List<ViewModel> mDataset;
        private View.OnClickListener mFooterOnClickListener;
        private static final int TYPE_FOOTER = 0;
        private static final int TYPE_ITEM = 1;

        public static class ViewModel {
            String email;

            ViewModel(String email) {
                this.email = email;
            }

            @Override
            public String toString() {
                return email;
            }
        }

        // Provide a reference to the views for each data item
        // Complex data items may need more than one view per item, and
        // you provide access to all the views for a data item in a view holder
        class ViewHolder extends RecyclerView.ViewHolder {
            public TextView mTextView;
            public ImageButton mDeleteButton;

            public ViewHolder(View itemView) {
                super(itemView);
                mTextView = (TextView) itemView.findViewById(R.id.create_key_email_item_email);
                mDeleteButton = (ImageButton) itemView.findViewById(R.id.create_key_email_item_delete_button);
            }
        }

        class FooterHolder extends RecyclerView.ViewHolder {
            public Button mAddButton;

            public FooterHolder(View itemView) {
                super(itemView);
                mAddButton = (Button) itemView.findViewById(R.id.create_key_add_email);
            }
        }

        // Provide a suitable constructor (depends on the kind of dataset)
        public EmailAdapter(List<ViewModel> myDataset, View.OnClickListener onFooterClickListener) {
            mDataset = myDataset;
            mFooterOnClickListener = onFooterClickListener;
        }

        // Create new views (invoked by the layout manager)
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            if (viewType == TYPE_FOOTER) {
                View v = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.create_key_email_list_footer, parent, false);
                return new FooterHolder(v);
            } else {
                //inflate your layout and pass it to view holder
                View v = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.create_key_email_list_item, parent, false);
                return new ViewHolder(v);
            }
        }

        // Replace the contents of a view (invoked by the layout manager)
        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, final int position) {
            if (holder instanceof FooterHolder) {
                FooterHolder thisHolder = (FooterHolder) holder;
                thisHolder.mAddButton.setOnClickListener(mFooterOnClickListener);
            } else if (holder instanceof ViewHolder) {
                ViewHolder thisHolder = (ViewHolder) holder;
                // - get element from your dataset at this position
                // - replace the contents of the view with that element
                final ViewModel model = mDataset.get(position);

                thisHolder.mTextView.setText(model.email);
                thisHolder.mDeleteButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        remove(model);
                    }
                });
            }
        }

        // Return the size of your dataset (invoked by the layout manager)
        @Override
        public int getItemCount() {
            return mDataset.size() + 1;
        }

        @Override
        public int getItemViewType(int position) {
            if (isPositionFooter(position)) {
                return TYPE_FOOTER;
            } else {
                return TYPE_ITEM;
            }
        }

        private boolean isPositionFooter(int position) {
            return position == mDataset.size();
        }

        public void add(String email) {
            mDataset.add(new ViewModel(email));
            notifyItemInserted(mDataset.size() - 1);
        }

        private void addAll(ArrayList<String> emails) {
            for (String email : emails) {
                mDataset.add(new EmailAdapter.ViewModel(email));
            }
        }

        public void remove(ViewModel model) {
            int position = mDataset.indexOf(model);
            mDataset.remove(position);
            notifyItemRemoved(position);
        }
    }

}
