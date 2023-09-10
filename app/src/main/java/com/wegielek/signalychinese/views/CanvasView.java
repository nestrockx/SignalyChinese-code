package com.wegielek.signalychinese.views;

import static androidx.constraintlayout.helper.widget.MotionEffect.TAG;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Toast;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.mlkit.common.MlKitException;
import com.google.mlkit.common.model.DownloadConditions;
import com.google.mlkit.common.model.RemoteModelManager;
import com.google.mlkit.vision.digitalink.DigitalInkRecognition;
import com.google.mlkit.vision.digitalink.DigitalInkRecognitionModel;
import com.google.mlkit.vision.digitalink.DigitalInkRecognitionModelIdentifier;
import com.google.mlkit.vision.digitalink.DigitalInkRecognizer;
import com.google.mlkit.vision.digitalink.DigitalInkRecognizerOptions;
import com.google.mlkit.vision.digitalink.Ink;
import com.wegielek.signalychinese.Interfaces.CanvasViewListener;
import com.wegielek.signalychinese.viewmodels.MainViewModel;

import java.util.ArrayList;

public class CanvasView extends View {

    private MainViewModel mainViewModel;
    private CanvasViewListener canvasViewListener;
    private static final int STROKE_WIDTH_DP = 3;
    private final Paint currentStrokePaint;
    private Canvas drawCanvas;
    private Bitmap canvasBitmap;
    private final Paint canvasPaint;
    private ArrayList<Ink.Stroke.Builder> strokesHistory;
    private Ink.Stroke.Builder strokeBuilder;

    private DigitalInkRecognitionModel model;
    private final RemoteModelManager remoteModelManager = RemoteModelManager.getInstance();
    private DigitalInkRecognizer recognizer;

    public CanvasView(Context context) {
        this(context, null);
    }

    public CanvasView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);

        currentStrokePaint = new Paint();
        currentStrokePaint.setColor(Color.WHITE);
        currentStrokePaint.setAntiAlias(true);
        currentStrokePaint.setStrokeWidth(
                TypedValue.applyDimension(
                        TypedValue.COMPLEX_UNIT_DIP, STROKE_WIDTH_DP, getResources().getDisplayMetrics()));
        currentStrokePaint.setStyle(Paint.Style.STROKE);
        currentStrokePaint.setStrokeJoin(Paint.Join.ROUND);
        currentStrokePaint.setStrokeCap(Paint.Cap.ROUND);
        canvasPaint = new Paint(Paint.DITHER_FLAG);
        strokesHistory = new ArrayList<>();
        init(100, 100);
    }

    public void setOnRecognizeListener(CanvasViewListener onRecognizeListener) {
        this.canvasViewListener = onRecognizeListener;
    }

    public void init(int width, int height, MainViewModel mainViewModel) {
        this.mainViewModel = mainViewModel;
        init(width, height);
    }

    private void init(int width, int height) {
        canvasBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        drawCanvas = new Canvas(canvasBitmap);
        drawCanvas.drawColor(Color.BLACK);
        invalidate();
        downloadModel();
    }

    public void clear() {
        mainViewModel.resetCurrentVisibleStroke();
        mainViewModel.setInkBuilder(Ink.builder());
        drawCanvas.drawColor(Color.BLACK);
        invalidate();
        clearHistory();
    }

    public void undo() {
        if (strokesHistory.size() > 0) {
            Ink.Builder newInkBuilder = new Ink.Builder();
            strokesHistory.remove(strokesHistory.size() - 1);
            for (int i = 0; i < strokesHistory.size(); i++) {
                newInkBuilder.addStroke(strokesHistory.get(i).build());
            }
            mainViewModel.setInkBuilder(newInkBuilder);
            if (strokesHistory.size() != 1) {
                recognizeWord();
            }
        }

        if (mainViewModel.getVisibleStrokeSize() > 0) {
            Path newCurrentVisibleStroke = new Path();
            mainViewModel.removeVisibleStroke(mainViewModel.getVisibleStrokeSize() - 1);
            for (int i = 0; i < mainViewModel.getVisibleStrokeSize(); i++) {
                newCurrentVisibleStroke.addPath(mainViewModel.getVisibleStroke(i));
            }
            mainViewModel.setCurrentVisibleStroke(newCurrentVisibleStroke);
            drawCanvas.drawColor(Color.BLACK);
            invalidate();
        }
    }

    private void clearHistory() {
        strokesHistory.clear();
        mainViewModel.clearVisibleStrokes();
    }

    public void recognizeWord() {
        isModelDownloaded().onSuccessTask(result -> {
            if(!result) {
                Toast.makeText(getContext(), "Model not downloaded yet", Toast.LENGTH_SHORT).show();
                return Tasks.forResult(null);
            }
            recognize();
            return Tasks.forResult(null);
        });
    }

    private void recognize() {
        recognizer.recognize(mainViewModel.inkBuilderBuild()).addOnSuccessListener(
                result -> {
                    canvasViewListener.onResults(result.getCandidates());
                }
        ).addOnFailureListener(
                e -> {
                    Toast.makeText(getContext(), "Error during recognition: " + e, Toast.LENGTH_SHORT).show();
                }
        );
    }

    private Task<Boolean> isModelDownloaded() {
        return remoteModelManager.isModelDownloaded(model);
    }

    private void downloadModel() {
        DigitalInkRecognitionModelIdentifier modelIdentifier;
        try {
            modelIdentifier =
                    DigitalInkRecognitionModelIdentifier.fromLanguageTag("zh-Hani-CN");
        } catch (MlKitException e) {
            throw new RuntimeException();
        }
        if (modelIdentifier == null) {
            Toast.makeText(getContext(), "Model not find", Toast.LENGTH_LONG).show();
            return;
        }

        model = DigitalInkRecognitionModel.builder(modelIdentifier).build();

        // Get a recognizer for the language
        recognizer =
                DigitalInkRecognition.getClient(
                        DigitalInkRecognizerOptions.builder(model).build());

        remoteModelManager
                .download(model, new DownloadConditions.Builder().build())
                .addOnSuccessListener(aVoid -> Log.i(TAG, "Model downloaded"))
                .addOnFailureListener(
                        e -> Log.e(TAG, "Error while downloading a model: " + e));
    }

    @Override
    protected void onDraw(Canvas canvas) {
        canvas.drawBitmap(canvasBitmap, 0, 0, canvasPaint);
        if (mainViewModel != null) {
            drawCanvas.drawPath(mainViewModel.getCurrentVisibleStroke(), currentStrokePaint);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int action = event.getActionMasked();
        float x = event.getX();
        float y = event.getY();
        long t = System.currentTimeMillis();

        switch (action) {
            case MotionEvent.ACTION_DOWN:
                mainViewModel.moveToCurrentVisibleStroke(x, y);
                strokeBuilder = Ink.Stroke.builder();
                strokeBuilder.addPoint(Ink.Point.create(x, y, t));
                break;
            case MotionEvent.ACTION_MOVE:
                mainViewModel.lineToCurrentVisibleStroke(x, y);
                strokeBuilder.addPoint(Ink.Point.create(x, y, t));
                break;
            case MotionEvent.ACTION_UP:
                mainViewModel.lineToCurrentVisibleStroke(x, y);
                mainViewModel.addVisibleStroke(new Path(mainViewModel.getCurrentVisibleStroke()));
                //mainViewModel.resetCurrentVisibleStroke();
                strokeBuilder.addPoint(Ink.Point.create(x, y, t));
                strokesHistory.add(strokeBuilder);
                mainViewModel.addInkBuilderStroke(strokeBuilder.build());
                strokeBuilder = null;
                recognizeWord();
                break;
            default:
                break;
        }
        invalidate();
        return true;
    }

    @Override
    public boolean performClick() {
        // Calls the super implementation, which generates an AccessibilityEvent
        // and calls the onClick() listener on the view, if any
        super.performClick();

        // Handle the action for the custom click here

        return true;
    }
}
