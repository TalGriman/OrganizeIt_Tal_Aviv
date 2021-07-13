package com.example.organizeit_tal_aviv;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;


public class RegisterActivity extends AppCompatActivity implements View.OnClickListener {
    EditText userEmailRegister,userPasswordRegister, userConfirmPasswordRegister; // אינפוטים לאימייל וסיסמה
    Button btnRegister; // כפתור להרשמה
    TextView loginLink; // לינק חזרה להתחברות
    FirebaseAuth mAuth; // הותנטיקציה של פיירבייס

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        initViews();
        initButton();
    }

    //אתחול המשתנים
    private void initViews() {
        userEmailRegister = findViewById(R.id.userEmailRegister);
        userPasswordRegister = findViewById(R.id.userPasswordRegister);
        userConfirmPasswordRegister = findViewById(R.id.userConfirmPasswordRegister);
        btnRegister = findViewById(R.id.btnRegister);
        loginLink = findViewById(R.id.linkLogin);
        mAuth = FirebaseAuth.getInstance();
    }

    // אתחול אירוע לחיצה על כפתור
    private void initButton() {
        btnRegister.setOnClickListener(this);
        loginLink.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId())
        {
            case R.id.btnRegister:
                register();
                break;
            case R.id.linkLogin:
                linkLogin();
                break;
        }
    }

    // מעבר חזרה להתחברות
    private void linkLogin() {
        startActivity(new Intent(RegisterActivity.this,MainActivity.class));
    }

    // הרשמה
    private void register() {
        // בדיקה שהשדות לא ריקים
        if(userEmailRegister.getText().toString().isEmpty() || userPasswordRegister.getText().toString().isEmpty())
        {
            Toast.makeText(getApplicationContext(),"fields cannot be empty",Toast.LENGTH_LONG).show();
            return;
        }
        // בדיקה שהסיסמה והאישור סיסמה זהים
        if (!userPasswordRegister.getText().toString().equals(userConfirmPasswordRegister.getText().toString()))
        {
            Toast.makeText(getApplicationContext(),"passwords doesn't match",Toast.LENGTH_LONG).show();
            return;
        }
        // יצירת משתמש בפיירבייס עם אימייל וסיסמה
        mAuth.createUserWithEmailAndPassword(userEmailRegister.getText().toString(),userPasswordRegister.getText().toString())
                .addOnCompleteListener(RegisterActivity.this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if(task.isSuccessful())
                        {
                            startActivity(new Intent(RegisterActivity.this,MainActivity.class));
                        }
                        else
                        {
                                Toast.makeText(getApplicationContext(),task.getException().getMessage(),Toast.LENGTH_LONG).show();
                        }
                    }
                });
    }

    // בכפתור חזור של הפלאפון תעביר לעמוד התחברות
    @Override
    public void onBackPressed() {
        startActivity(new Intent(RegisterActivity.this,MainActivity.class));
    }
}