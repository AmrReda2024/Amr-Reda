
import { GoogleGenAI, GenerateContentResponse, Part } from "@google/genai";
import { GEMINI_MODEL_NAME, GEMINI_SYSTEM_INSTRUCTION } from '../constants';
import { Source } from '../types';

// IMPORTANT: API Key must be set in the environment variables as API_KEY
// For example, in a Node.js environment or a .env file: API_KEY=your_actual_api_key
// This application assumes process.env.API_KEY is available.
const API_KEY = process.env.API_KEY;

if (!API_KEY) {
  console.error("API_KEY environment variable not set. Please set it to use the Gemini API.");
  // You could throw an error here or handle it in the UI, 
  // but for this example, we'll log and proceed, calls will fail.
}

const ai = new GoogleGenAI({ apiKey: API_KEY || "MISSING_API_KEY" });

interface GeminiServiceResponse {
  text: string;
  sources: Source[];
}

export const fetchGeminiResponse = async (prompt: string): Promise<GeminiServiceResponse> => {
  try {
    const contents: Part[] = [{ text: prompt }];
    
    const response: GenerateContentResponse = await ai.models.generateContent({
      model: GEMINI_MODEL_NAME,
      contents: contents,
      config: {
        systemInstruction: GEMINI_SYSTEM_INSTRUCTION,
        tools: [{ googleSearch: {} }], // Enable Google Search grounding
      },
    });

    const text = response.text;
    
    let sources: Source[] = [];
    const groundingMetadata = response.candidates?.[0]?.groundingMetadata;
    if (groundingMetadata?.groundingChunks) {
      sources = groundingMetadata.groundingChunks
        .filter(chunk => chunk.web && chunk.web.uri && chunk.web.title)
        .map(chunk => ({
          uri: chunk.web!.uri!, // Non-null assertion as we filtered
          title: chunk.web!.title!, // Non-null assertion
        }));
    }

    return { text, sources };
  } catch (error) {
    console.error("Error fetching Gemini response:", error);
    // It's good practice to check for specific error types from the SDK if available
    // For now, rethrow a generic message
    if (error instanceof Error) {
        if (error.message.includes("API key not valid")) {
             throw new Error("The API key is invalid. Please check your API_KEY environment variable.");
        }
         throw new Error(`Gemini API error: ${error.message}`);
    }
    throw new Error("An unknown error occurred while fetching data from Gemini API.");
  }
};
    