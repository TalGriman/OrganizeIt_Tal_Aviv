package com.example.organizeit_tal_aviv;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.login.LoginManager;
import com.facebook.login.LoginResult;
import com.facebook.login.widget.LoginButton;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FacebookAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.auth.UserInfo;

public class
MainActivity extends AppCompatActivity implements View.OnClickListener {

    EditText userEmailLogin, userPasswordLogin;
    Button btnLogin , btnFacebook;
    TextView resetPassword, register;
    FirebaseAuth mAuth;
    CallbackManager callbackManager;
    LoginButton fb_login;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initViews();
        initButton();
        initallizeFacebook();
    }

    // בודק האם אנחנו כבר מחוברים ובהתאם מעביר למסך התחברות או למסך התיקיות
    @Override
    protected void onStart() {
        super.onStart();
        if (mAuth.getCurrentUser() != null) {
            startActivity(new Intent(MainActivity.this, WelcomeActivity.class));
        }
    }

    // מונע קריסה בלחיצה על back במסך ההתחברות
    @Override
    public void onBackPressed() {
        return;
    }


    private void initViews() {
        userEmailLogin = findViewById(R.id.userEmailLogin); // אינפוט של האימייל
        userPasswordLogin = findViewById(R.id.userPasswordLogin); // אינפוט של הסיסמא
        btnLogin = findViewById(R.id.btnLogin);
        resetPassword = findViewById(R.id.linkResetPassword);
        register = findViewById(R.id.linkRegister);
        mAuth = FirebaseAuth.getInstance(); // אובייקט איתו מתקשרים עם האוטנטיקציה של פיירבייס
        fb_login = findViewById(R.id.fb_login);  // כפתור התחברות עם פייסבוק
        btnFacebook = findViewById(R.id.btnFacebook);
    }


    // אתחול אירוע לחיצה על הכפתורים
    private void initButton() {
        btnLogin.setOnClickListener(this);
        resetPassword.setOnClickListener(this);
        register.setOnClickListener(this);
        btnFacebook.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btnLogin:
                login();
                break;
            case R.id.btnFacebook:
                fb_login.performClick(); // שימוש בפונקציונליות של כפתור פייסבוק עבור הכפתור המעוצב שלנו
                break;
            case R.id.linkResetPassword:
                resetPass(v);
                break;
            case R.id.linkRegister:
                registerLink();
                break;
        }
    }


    // התחברות עם מייל וסיסמא
    private void login() {
        // לוודא ששדות האימייל והסיסמא אינם ריקים
        if (userEmailLogin.getText().toString().isEmpty() || userPasswordLogin.getText().toString().isEmpty()) {
            Toast.makeText(getApplicationContext(), "Email and Password cannot be empty", Toast.LENGTH_SHORT).show();
            return;
        }
        // התחברות עם האימייל והסיסמה
        mAuth.signInWithEmailAndPassword(userEmailLogin.getText().toString(), userPasswordLogin.getText().toString()).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
            @Override
            public void onComplete(@NonNull Task<AuthResult> task) {
                if (task.isSuccessful()) {
                    // בהשלמת ההתחברות לעבור למסך התיקיות
                    startActivity(new Intent(MainActivity.this, WelcomeActivity.class));
                } else {
                    // אם לא הצליחה ההתחברות, להציג את השגיאה
                    Toast.makeText(getApplicationContext(), task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    // אתחול כפתור הפייסבוק המקורי. הגדרת הרשאות ופונקציה שתבוצע לאחר קבלת הרשאות
    private void initallizeFacebook() {
        callbackManager = CallbackManager.Factory.create(); // משמש לתקשורת עם פייסבוק
        fb_login.setPermissions("email", "public_profile"); // מה שאנחנו רוצים לקבל מפייסבוק
        fb_login.registerCallback(callbackManager, new FacebookCallback<LoginResult>() {
            @Override
            public void onSuccess(LoginResult loginResult) {
                Log.d("Facebook", "On Success");
                handleFacebookLogin(loginResult);
            }

            @Override
            public void onCancel() {
                Log.d("Facebook", "On Cancel");
            }

            @Override
            public void onError(FacebookException error) {

                Log.d("Facebook", "On Error");
                Toast.makeText(getApplicationContext(),error.getMessage(),Toast.LENGTH_LONG).show();
            }
        });
    }

    // הפונקציה שמבוצעת לאחר קבלת הרשאות פייסבוק ומחברת את המשתמש לפייר-בייס
    private void handleFacebookLogin(LoginResult loginResult) {
        // אובייקט של פיירבייס שמשמש לאימות פרטי משתמש
        AuthCredential credential = FacebookAuthProvider.getCredential(loginResult.getAccessToken().getToken());
        // התחברות לפיירבייס עם פרטי המשתמש שקיבלנו מפייסבוק
        mAuth.signInWithCredential(credential).addOnCompleteListener(MainActivity.this, new OnCompleteListener<AuthResult>() {
            @Override
            public void onComplete(@NonNull Task<AuthResult> task) {
                if (task.isSuccessful()) {
                    // מביא את היוזר המחובר על מנת להציגו בלוגים
                    FirebaseUser user = mAuth.getCurrentUser();
                    // הצגת לוג
                    Log.d("Login success", "Success");
                    Log.d("User ", user.getUid());
                    startActivity(new Intent(MainActivity.this, WelcomeActivity.class));
                } else {
                    Log.d("Login", "Error");
                    Toast.makeText(getApplicationContext(),"Authentication failed.",Toast.LENGTH_LONG).show();
                    // כדי שהכפתור לא ישאר במצב מחובר
                    if (LoginManager.getInstance() != null) {
                        LoginManager.getInstance().logOut();
                    }
                }
            }
        });
    }

    // בסיום ההתחברות עם פייסבוק העברת תוצאת ההתחברות ל-LoginManager באמצעות ה-callbackManager
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        callbackManager.onActivityResult(requestCode, resultCode, data);
    }


    // מעבר להרשמה
    private void registerLink() {
        startActivity(new Intent(MainActivity.this, RegisterActivity.class));
    }


    // איפוס סיסמא
    private void resetPass(View v) {
        EditText resetMail = new EditText(v.getContext()); // מייצר אינפוט להכנסת המייל לאיפוס סיסמא
        resetMail.setInputType(InputType.TYPE_CLASS_TEXT
                | InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS); // מציג מקלדת שמתאימה למייל
        final AlertDialog.Builder passwordResetDialog = new AlertDialog.Builder(v.getContext()); // יוצר חלונית קופצת
        passwordResetDialog.setTitle("Reset Password?");
        passwordResetDialog.setMessage("Enter your email to get reset link.");
        passwordResetDialog.setView(resetMail);

        // מה שקורה אם לוחצים על yes
        passwordResetDialog.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String mail = resetMail.getText().toString();
                // שליחת מייל איפוס סיסמא
                mAuth.sendPasswordResetEmail(mail).addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Toast.makeText(getApplicationContext(), "Reset link sent to your email.", Toast.LENGTH_SHORT).show();
                    }
                }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        // מטעמי אבטחת מידע, גם אם שלחנו וגם אם לא שלחנו נקבל את אותה ההודעה.
                        Toast.makeText(getApplicationContext(), "Reset link sent to your email.", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });

        // מה שקורה אם לוחצים על no
        passwordResetDialog.setNegativeButton("No", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // לא קורה כלום ופשוט נסגר החלון
            }
        });

        passwordResetDialog.create().show(); // הצגת החלונית
    }

}