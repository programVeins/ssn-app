package in.edu.ssn.ssnapp;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.crashlytics.android.Crashlytics;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import in.edu.ssn.ssnapp.utils.FCMHelper;
import in.edu.ssn.ssnapp.utils.SharedPref;
import pl.droidsonroids.gif.GifImageView;

public class LogoutActivity extends AppCompatActivity {

    CardView signInCV;
    FirebaseAuth mAuth;
    GoogleSignInClient mGoogleSignInClient;
    private static int RC_SIGN_IN = 111;
    GifImageView progress;
    TextView tv_msg;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_logout);

        signInCV = findViewById(R.id.signInCV);
        tv_msg = findViewById(R.id.tv_msg);
        progress = findViewById(R.id.progress);

        Boolean flag = getIntent().getBooleanExtra("is_log_in",false);
        if(flag)
            tv_msg.setText("Start exploring new feeds.");
        else
            tv_msg.setText("You were successfully signed out.");

        initGoogleSignIn();

        signInCV.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mAuth.signOut();
                mGoogleSignInClient.signOut();
                Intent signInIntent = mGoogleSignInClient.getSignInIntent();
                startActivityForResult(signInIntent, RC_SIGN_IN);
            }
        });
    }

    /************************************************************************/
    // Google Signin
    public void initGoogleSignIn() {
        mAuth = FirebaseAuth.getInstance();
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).requestIdToken(getString(R.string.default_web_client_id)).requestEmail().build();
        mGoogleSignInClient = GoogleSignIn.getClient(LogoutActivity.this, gso);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RC_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                final GoogleSignInAccount acct = task.getResult(ApiException.class);
                Pattern pat = Pattern.compile("@[a-z]{2,8}(.ssn.edu.in)$");
                Matcher m = pat.matcher(acct.getEmail());

                //TODO: check for faculty regex

                if (m.find()) {
                    //Student only

                    AuthCredential credential = GoogleAuthProvider.getCredential(acct.getIdToken(), null);
                    progress.setVisibility(View.VISIBLE);
                    mAuth.signInWithCredential(credential).addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                        @Override
                        public void onComplete(@NonNull Task<AuthResult> task) {
                            if (task.isSuccessful()) {
                                Log.d("test_set", "signInWithCredential:success");
                                FirebaseUser user = mAuth.getCurrentUser();
                                checkForSignin(user,0);
                            } else {
                                Log.d("test_set", "signInWithCredential:failure");
                                progress.setVisibility(View.GONE);
                            }
                        }
                    });
                }
                else {
                    //Faculty only

                    AuthCredential credential = GoogleAuthProvider.getCredential(acct.getIdToken(), null);
                    progress.setVisibility(View.VISIBLE);
                    mAuth.signInWithCredential(credential).addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                        @Override
                        public void onComplete(@NonNull Task<AuthResult> task) {
                            if (task.isSuccessful()) {
                                Log.d("test_set", "signInWithCredential:success");
                                FirebaseUser user = mAuth.getCurrentUser();
                                checkForSignin(user,1);
                            } else {
                                Log.d("test_set", "signInWithCredential:failure");
                                progress.setVisibility(View.GONE);
                            }
                        }
                    });
                }
                //else
                //Toast.makeText(this, "Please use SSN mail ID", Toast.LENGTH_SHORT).show();
            } catch (ApiException e) {
                Log.d("test_set", e.getMessage());
                Crashlytics.log("stackTrace: "+e.getStackTrace()+" \n Error: "+e.getMessage());
            }
        }
    }

    public void checkForSignin(final FirebaseUser user, final int clearance) {
        String id = user.getUid();

        FirebaseFirestore.getInstance().collection("user").whereEqualTo("id", id).get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
            @Override
            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                if (task.isSuccessful()) {
                    if(clearance==0) {
                        //Student only
                        if (task.getResult().isEmpty())
                            signUpStudent(user);
                        else {
                            List<DocumentSnapshot> document = task.getResult().getDocuments();
                            signIn(user, document.get(0), clearance);
                        }
                    }
                    else{
                        //Faculty only
                        if (task.getResult().isEmpty()) {
                            FirebaseFirestore.getInstance().collection("faculty").whereEqualTo("email", user.getEmail()).get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                                @Override
                                public void onComplete(@NonNull Task<QuerySnapshot> task) {
                                    if(task.isSuccessful()) {
                                        if (!task.getResult().getDocuments().isEmpty()) {
                                            DocumentSnapshot document = task.getResult().getDocuments().get(0);
                                            signUpFaculty(user, document);
                                        } else {
                                            progress.setVisibility(View.GONE);
                                            Toast.makeText(LogoutActivity.this, "Please contact Admin!", Toast.LENGTH_SHORT).show();
                                        }
                                    }
                                }
                            });
                        }
                        else {
                            List<DocumentSnapshot> document = task.getResult().getDocuments();
                            signIn(user, document.get(0), clearance);
                        }
                    }
                }
            }
        });
    }

    /*****************************************************************/
    //Student signin and signup

    public void signIn(FirebaseUser user, DocumentSnapshot document, int clearance) {
        String dept = (String) document.get("dept");
        String email = user.getEmail();
        String id = (String) document.get("id");
        String name = (String) document.get("name");
        String access = (String) document.get("access");

        if(clearance==1){
            String position = (String) document.get("position");
            SharedPref.putString(getApplicationContext(), "position", position);
        }
        else{
            Long year = (Long) document.get("year");
            SharedPref.putInt(getApplicationContext(),"year", Integer.parseInt(year.toString()));
        }

        SharedPref.putInt(getApplicationContext(),"clearance",clearance);
        SharedPref.putString(getApplicationContext(),"dept", dept);
        SharedPref.putString(getApplicationContext(),"email", email);
        SharedPref.putString(getApplicationContext(),"id", id);
        SharedPref.putString(getApplicationContext(),"name", name);
        SharedPref.putString(getApplicationContext(), "access", access);
        SharedPref.putBoolean(getApplicationContext(),"is_logged_in", true);

        Log.d("test_set", "signin");
        progress.setVisibility(View.GONE);
        FCMHelper.SubscribeToTopic(this, dept);
        setUpNotification();
        FCMHelper.UpdateFCM(this, SharedPref.getString(this, "FCMToken"));

        if(clearance==1) {
            startActivity(new Intent(getApplicationContext(), FacultyHomeActivity.class));
            finish();
        }
        else {
            startActivity(new Intent(getApplicationContext(), StudentHomeActivity.class));
            finish();
        }
    }

    public void signUpStudent(FirebaseUser user) {
        String email = user.getEmail();
        String id = user.getUid();

        String[] split = email.split("@");
        int y = split[1].indexOf(".");
        String dept = split[1].substring(0, y);

        String dp_url = user.getPhotoUrl().toString();
        String name = user.getDisplayName();

        //TODO: Handle break year students accordingly by [admin]
        int year = Integer.parseInt(split[0].substring(split[0].length() - 5, split[0].length() - 3)) + 2000;
        if (year <= 2016)
            year = 2016;

        Map<String, Object> users = new HashMap<>();
        users.put("access", "");
        users.put("clearance", 0);
        users.put("dept", dept);
        users.put("dp_url", dp_url);
        users.put("email", email);
        users.put("id", id);
        users.put("name", name);
        users.put("year", year);
        users.put("FCMToken", SharedPref.getString(this, "FCMToken"));
        FirebaseFirestore.getInstance().collection("user").document(id).set(users);
        FCMHelper.SubscribeToTopic(this, dept);
        setUpNotification();

        SharedPref.putInt(getApplicationContext(), "clearance", 0);
        SharedPref.putString(getApplicationContext(), "dept", dept);
        SharedPref.putString(getApplicationContext(), "email", email);
        SharedPref.putString(getApplicationContext(), "id", id);
        SharedPref.putString(getApplicationContext(), "name", name);
        SharedPref.putInt(getApplicationContext(), "year", year);
        SharedPref.putBoolean(getApplicationContext(), "is_logged_in", true);
        Log.d("test_set", "signup");
        progress.setVisibility(View.GONE);
        startActivity(new Intent(getApplicationContext(), StudentHomeActivity.class));
        finish();
    }

    public void signUpFaculty(FirebaseUser user, DocumentSnapshot document) {
        String email = user.getEmail();
        String id = user.getUid();
        String dept = document.getString("dept");
        String access = document.getString("access");
        String position = document.getString("position");
        String name = document.getString("name");
        String dp_url = user.getPhotoUrl().toString();

        Map<String, Object> users = new HashMap<>();
        users.put("access", access);
        users.put("position", position);
        users.put("clearance", 1);
        users.put("dept", dept);
        users.put("dp_url", dp_url);
        users.put("email", email);
        users.put("id", id);
        users.put("name", name);
        users.put("FCMToken", SharedPref.getString(this, "FCMToken"));
        FirebaseFirestore.getInstance().collection("user").document(id).set(users);
        FCMHelper.SubscribeToTopic(this, dept);
        setUpNotification();

        SharedPref.putInt(getApplicationContext(), "clearance", 1);
        SharedPref.putString(getApplicationContext(), "email", email);
        SharedPref.putString(getApplicationContext(), "id", id);
        SharedPref.putString(getApplicationContext(), "position", position);
        SharedPref.putString(getApplicationContext(), "access", access);
        SharedPref.putString(getApplicationContext(), "dept", dept);
        SharedPref.putString(getApplicationContext(), "name", name);
        SharedPref.putBoolean(getApplicationContext(), "is_logged_in", true);

        Log.d("test_set", "signup");
        progress.setVisibility(View.GONE);
        startActivity(new Intent(getApplicationContext(), FacultyHomeActivity.class));
        finish();
    }

    //*****************************************************************************************************************************

    public void setUpNotification() {
        SharedPref.putBoolean(getApplicationContext(), "switch_all", true);
        SharedPref.putBoolean(getApplicationContext(), "switch_dept", true);
        SharedPref.putBoolean(getApplicationContext(), "switch_bus", true);
        SharedPref.putBoolean(getApplicationContext(), "switch_club", true);
        SharedPref.putBoolean(getApplicationContext(), "switch_exam", true);
        SharedPref.putBoolean(getApplicationContext(), "switch_workshop", true);
    }

    @Override
    public void onBackPressed() {
        Intent startMain = new Intent(Intent.ACTION_MAIN);
        startMain.addCategory(Intent.CATEGORY_HOME);
        startMain.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(startMain);
        finish();
    }
}
