
//Doesn't do anything (just navigation page to start "Go to location")


package com.example.myapplication;

import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.myapplication.R;

public class FindLocationActivity extends AppCompatActivity {

    private Button findButton;
        
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        
        setContentView(R.layout.find_location);

        findButton = findViewById(R.id.findButton);

        findButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                navigateToNavigationActivity();
            }
        });
    }
    
    private void navigateToNavigationActivity() {
        Location startLocation = null;
        Intent intent = getIntent();
        if (intent != null && intent.hasExtra("startLocation")) {
            startLocation = intent.getParcelableExtra("startLocation");
        }
        Intent intents = new Intent(FindLocationActivity.this, NavigationActivity.class);
        if(startLocation != null){
            intents.putExtra("startLocation", startLocation);
        }
        startActivity(intents);
    }
}
