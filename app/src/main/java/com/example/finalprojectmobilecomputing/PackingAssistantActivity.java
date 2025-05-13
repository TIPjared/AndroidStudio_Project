package com.example.finalprojectmobilecomputing;

import android.os.Bundle;
import android.text.Html;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.BulletSpan;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.google.ai.client.generativeai.GenerativeModel;
import com.google.ai.client.generativeai.java.GenerativeModelFutures;
import com.google.ai.client.generativeai.type.Content;
import com.google.ai.client.generativeai.type.GenerateContentResponse;
import com.google.android.material.textfield.TextInputEditText;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PackingAssistantActivity extends AppCompatActivity {

    private TextInputEditText userInputField;
    private Button submitButton;
    private TextView aiResponse;
    private ImageButton backButton;
    private View footerInfo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_packing_assistant);
        
        // Initialize UI components
        userInputField = findViewById(R.id.user_input);
        submitButton = findViewById(R.id.submit_button);
        aiResponse = findViewById(R.id.ai_response);
        backButton = findViewById(R.id.backButton);
        footerInfo = findViewById(R.id.footer_info);

        // Set up back button
        backButton.setOnClickListener(v -> {
            onBackPressed();
        });

        // Set up submit button
        submitButton.setOnClickListener(v -> {
            String userDescription = userInputField.getText().toString().trim();
            
            if (userDescription.isEmpty()) {
                Toast.makeText(this, "Please enter trip details", Toast.LENGTH_SHORT).show();
                return;
            }
            
            // Show loading indicator
            aiResponse.setText("Generating packing list...");
            footerInfo.setVisibility(View.GONE);
            
            // Generate AI recommendation
            generatePackingList(userDescription);
        });
    }
    
    /**
     * Formats the AI-generated packing list to look more professional
     */
    private String formatPackingList(String rawText) {
        // First, remove all instances of asterisks completely
        String text = rawText.replaceAll("\\*\\*", "");
        
        // Replace asterisk bullet points with proper bullet points
        text = text.replaceAll("\\* ", "• ");
        text = text.replaceAll("\\*", "");
        
        // Process the text line by line for better control
        String[] lines = text.split("\n");
        StringBuilder formattedText = new StringBuilder();
        
        String currentCategory = "";
        boolean isFirstCategory = true;
        
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].trim();
            
            if (line.isEmpty()) continue;
            
            // Check if this is a category header
            if (line.matches("^[A-Z][\\w\\s]+(:|$).*")) {
                String header = line.replaceAll(":\\s*$", ""); // Remove trailing colon
                currentCategory = header;
                
                // Add some extra spacing before categories (except first one)
                if (!isFirstCategory) {
                    formattedText.append("<br><br>");
                } else {
                    isFirstCategory = false;
                }
                
                // Add category with clean, bold styling
                formattedText.append("<span style='color:#000000; font-size:18px;'><b>")
                        .append(header)
                        .append("</b></span><br>");
            } 
            // Check if this line starts with a bullet point or needs one
            else {
                String itemText = line;
                if (line.startsWith("•")) {
                    itemText = line.substring(1).trim();
                }
                
                // Split and format the item properly
                ItemParts parts = splitItemIntoParts(itemText);
                
                // Format item with proper bullet and styling - using the bullet character directly
                formattedText.append("<span style='margin-left:8px;'>• ")
                        .append(parts.name);
                
                // Add quantity in the same style
                if (!parts.quantity.isEmpty()) {
                    formattedText.append(" ")
                            .append(parts.quantity);
                }
                
                formattedText.append("</span><br>");
            }
        }
        
        String result = formattedText.toString();
        
        // Add clean styling
        result = "<span style='color:#000000; line-height:1.3;'>" + result + "</span>";
        
        return result;
    }
    
    /**
     * Helper class to store item name and quantity
     */
    private static class ItemParts {
        String name;
        String quantity;
        
        ItemParts(String name, String quantity) {
            this.name = name;
            this.quantity = quantity;
        }
    }
    
    /**
     * Intelligently splits an item into name and quantity parts
     */
    private ItemParts splitItemIntoParts(String itemText) {
        // Special case for handling parenthetical descriptions
        if (itemText.contains("(")) {
            Pattern p = Pattern.compile("(.+?)\\s+(\\(.+?\\))");
            Matcher m = p.matcher(itemText);
            
            if (m.matches()) {
                String mainText = m.group(1);
                String parentheticalText = m.group(2);
                
                // Check if the main text has a quantity
                ItemParts mainParts = extractQuantity(mainText);
                if (!mainParts.quantity.isEmpty()) {
                    return new ItemParts(mainParts.name, mainParts.quantity + " " + parentheticalText);
                } else {
                    return new ItemParts(mainText, parentheticalText);
                }
            }
        }
        
        return extractQuantity(itemText);
    }
    
    /**
     * Extract quantity from text using regex patterns
     */
    private ItemParts extractQuantity(String text) {
        // Define common quantity patterns
        String[] quantityPatterns = {
            "(\\d+)-(\\d+)", // e.g., "2-3"
            "(\\d+)\\s+pairs?", // e.g., "3 pairs"
            "(\\d+)\\s+sets?", // e.g., "2 sets"
            "(\\d+)", // Just a number
        };
        
        for (String pattern : quantityPatterns) {
            Pattern p = Pattern.compile("(.+?)\\s+(" + pattern + ".*)");
            Matcher m = p.matcher(text);
            
            if (m.matches()) {
                return new ItemParts(m.group(1), m.group(2));
            }
        }
        
        // If no quantity pattern matches, just return the whole item as name
        return new ItemParts(text, "");
    }
    
    /**
     * Returns an appropriate icon for each category
     */
    private String getCategoryIcon(String category) {
        category = category.toLowerCase();
        
        if (category.contains("cloth") || category.contains("wear") || category.contains("apparel")) {
            return "👕"; // Clothing 
        } else if (category.contains("toilet") || category.contains("hygiene") || category.contains("bathroom")) {
            return "🧴"; // Toiletries
        } else if (category.contains("tech") || category.contains("electronic") || category.contains("gadget")) {
            return "📱"; // Electronics
        } else if (category.contains("document") || category.contains("paper") || category.contains("id")) {
            return "📄"; // Documents
        } else if (category.contains("medicine") || category.contains("health") || category.contains("first aid")) {
            return "💊"; // Medicine
        } else if (category.contains("food") || category.contains("snack") || category.contains("drink")) {
            return "🍎"; // Food
        } else if (category.contains("accessory") || category.contains("accessories")) {
            return "👓"; // Accessories
        } else if (category.contains("shoe") || category.contains("footwear")) {
            return "👟"; // Shoes
        } else if (category.contains("beach") || category.contains("swim")) {
            return "🏖️"; // Beach
        } else if (category.contains("outdoor") || category.contains("hiking") || category.contains("camping")) {
            return "🏕️"; // Outdoor
        } else if (category.contains("baby") || category.contains("kid") || category.contains("child")) {
            return "👶"; // Children
        } else {
            return "📋"; // Default list icon
        }
    }

    /**
     * Enhance the prompt to get better-formatted results from the AI
     */
    private String buildEnhancedPrompt(String userInput) {
        return "Create a detailed packing list for this trip: " + userInput + 
            "\n\nPlease format your response following these exact rules:" +
            "\n1. Organize items by clear categories (e.g., Clothing, Toiletries, Electronics)" +
            "\n2. Make each category name a single word on its own line" + 
            "\n3. Format each item on its own line with a bullet point" +
            "\n4. Include quantities for each item (e.g., '2-3 pairs')" +
            "\n5. For clothing items, add a brief description in parentheses if relevant" +
            "\n6. Keep item descriptions concise" +
            "\n7. Use this format for your response:" +
            "\n\nClothing" +
            "\n• Shirts 5-7 (variety)" +
            "\n• Pants 2-3 pairs (jeans, chinos)" +
            "\n• Underwear 7 pairs" +
            "\n\nToiletries" +
            "\n• Toothbrush 1" +
            "\n• Toothpaste 1 tube";
    }
    
    private void generatePackingList(String userInput) {
        // Use the enhanced prompt builder
        String enhancedPrompt = buildEnhancedPrompt(userInput);

        String API_KEY = "AIzaSyB3haers8c6B7dIPbk3IiBTz6uHDJMUM5k";

        GenerativeModel gmodel = new GenerativeModel("gemini-2.0-flash-lite", API_KEY);
        GenerativeModelFutures modelFutures = GenerativeModelFutures.from(gmodel);

        Content content = new Content.Builder()
                .addText(enhancedPrompt)
                .build();

        // Make API request
        ListenableFuture<GenerateContentResponse> response = modelFutures.generateContent(content);
        
        // Handle response
        Futures.addCallback(response, new FutureCallback<GenerateContentResponse>() {
            @Override
            public void onSuccess(GenerateContentResponse result) {
                // Update UI on the main thread
                runOnUiThread(() -> {
                    String rawPackingList = result.getText().trim();
                    String formattedPackingList = formatPackingList(rawPackingList);
                    aiResponse.setText(Html.fromHtml(formattedPackingList, Html.FROM_HTML_MODE_COMPACT));
                    
                    // Show the tip footer
                    footerInfo.setVisibility(View.VISIBLE);
                });
            }

            @Override
            public void onFailure(Throwable t) {
                // Handle error on the main thread
                runOnUiThread(() -> {
                    aiResponse.setText("Failed to generate packing list. Please try again.");
                    Toast.makeText(PackingAssistantActivity.this, 
                            "Error: " + t.getMessage(), 
                            Toast.LENGTH_SHORT).show();
                    
                    // Hide the tip footer on error
                    footerInfo.setVisibility(View.GONE);
                });
            }
        }, MoreExecutors.directExecutor());
    }
}