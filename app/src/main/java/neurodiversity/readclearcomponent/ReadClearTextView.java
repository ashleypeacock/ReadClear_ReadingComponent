package neurodiversity.readclearcomponent;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.Build;
import android.speech.tts.TextToSpeech;
import android.text.Editable;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.SpannedString;
import android.text.TextWatcher;
import android.text.style.ForegroundColorSpan;
import android.text.style.LineBackgroundSpan;
import android.util.AttributeSet;
import android.util.Log;

import static android.content.ContentValues.TAG;

/**
 * The text view that is used in ReadClear.
 * @author ashley
 **/

public class ReadClearTextView extends android.support.v7.widget.AppCompatTextView {

    private int currentLineNumber = 0;
    PageBoundsReachedListener listener;

    private int numberOfLinesInBox = 1;
    private int wordCountPerLine = 8;

    private int fixationboxColor = getResources().getColor(R.color.fixationBoxColorDefault);
    private int fontColor = getResources().getColor(R.color.fixationboxTextColorDefault);
    private float opacity = 5;

    public ReadClearTextView(Context context) {
        super(context);
        init();
    }

    public ReadClearTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
        TypedArray a = context.getTheme().obtainStyledAttributes(attrs, R.styleable.ReadClearTextView, 0, 0);
        try {
            opacity = a.getInteger(R.styleable.ReadClearTextView_rcOpacity, 5);
            fontColor = a.getColor(R.styleable.ReadClearTextView_rctextColor, getResources().getColor(R.color.fixationboxTextColorDefault));
            fixationboxColor = a.getColor(R.styleable.ReadClearTextView_rcfixationboxColor, getResources().getColor(R.color.fixationBoxColorDefault));
            numberOfLinesInBox = a.getInteger(R.styleable.ReadClearTextView_rcboxLineCount, 1);
            wordCountPerLine = a.getColor(R.styleable.ReadClearTextView_rcboxWordsPerLine, 8);
        } finally {
            a.recycle();
        }
        init();
    }

    public ReadClearTextView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        addTextChangedListener(new TextWatcher() {

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                updateView();
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count,
                                          int after) {
            }

            @Override
            public void afterTextChanged(Editable s) {

            }

        });
    }

    /**
     * Speak out loud the current highlighted line.
     * @param tts
     */
    public void speakCurrentLine(TextToSpeech tts) {
        String text = getTextAtCurrentLine();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, null);
        } else {
            tts.speak(text, TextToSpeech.QUEUE_FLUSH, null);
        }
    }

    public boolean highlightNextLine() {
        if(currentLineNumber <= getLineCount() - 1) {
            currentLineNumber+= numberOfLinesInBox;
            if(hasMoreText()) {
                highlightLine(currentLineNumber);
                return true;
            } else {
                if(listener != null) {
                    listener.endOfPage();
                }
            }
        } else {
            if(listener != null) {
                listener.endOfPage();
            }
        }
        return false;
    }

    public void highlightPreviousLine() {
        if(currentLineNumber <= 0) {
            if(listener != null) {
                if(listener != null) {
                    listener.startOfPage();
                }
            }
        } else {
            currentLineNumber-= numberOfLinesInBox;
            highlightLine(currentLineNumber);
        }
    }

    public void highlightLine(int lineNumber)  {
        highlightLine(lineNumber, numberOfLinesInBox);
    }

    /**
     * @param highlightedLineNumber
     * @param n number of lines to highlight
     */
    public void highlightLine(int highlightedLineNumber, int n)  {
        if(highlightedLineNumber < 0 || highlightedLineNumber >= getLineCount() - 1 || n <= 0) {
            Log.e(TAG, "highlightLine: Out of bounds: n is " + n + ", linenumber is: " + highlightedLineNumber);
            return;
        }

        int start = getLayout().getLineStart(highlightedLineNumber);
        int end = getLayout().getLineEnd(highlightedLineNumber + n - 1);

        if(highlightedLineNumber + n - 1 >= getLineCount()) {
            end = getLayout().getLineEnd(getLineCount() - 1);
        }

        FixationBoxColorSpan[] colorSpans = ((SpannedString)getText()).getSpans(0, getText().length(), FixationBoxColorSpan.class);
        Spannable spanna = new SpannableString(getText());
        if(colorSpans.length > 0) {
            for(FixationBoxColorSpan spans : colorSpans) {
                spanna.removeSpan(spans);
            }
        }

        ForegroundColorSpan[] foreSpans = ((SpannedString)getText()).getSpans(0, getText().length(), ForegroundColorSpan.class);
        if(colorSpans.length > 0) {
            for(ForegroundColorSpan spans : foreSpans) {
                spanna.removeSpan(spans);
            }
        }

        int transparent = Color.argb(Math.round(Color.alpha(fontColor) * opacity), Color.red(fontColor), Color.green(fontColor), Color.blue(fontColor));

        spanna.setSpan(new FixationBoxColorSpan(fixationboxColor), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        spanna.setSpan(new ForegroundColorSpan(fixationboxColor), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        spanna.setSpan(new ForegroundColorSpan(transparent), 0, start, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        spanna.setSpan(new ForegroundColorSpan(transparent), end, getText().length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

        setText(spanna);
    }

    public void setNumberOfLinesInBox(int numberOfLinesInBox) {
        this.numberOfLinesInBox = numberOfLinesInBox;
    }

    public String getTextAtLine(int lineNumber) {
        if (lineNumber < 0 || getLayout() == null || lineNumber > getLayout().getLineCount() -1) {
            return null;
        }

        if(getLayout() != null) {
            int start = getLayout().getLineStart(lineNumber);
            int end = getLayout().getLineEnd(lineNumber);

            if (start >= 0 && end <= getText().toString().length()) {
                String textAtLine = getText().toString().substring(start, end);
                if(!textAtLine.isEmpty()) {
                    return textAtLine;
                }
            }
        }

        return null;
    }

    public String getTextAtCurrentLine() {
        return getTextAtLine(currentLineNumber);
    }

    public int getCurrentLineNumber() {
        return currentLineNumber;
    }

    /**
     * Gets the line number given the character position
     * @param characterPosition
     * @return line number given the position.
     */
    public int getLineNumber(int characterPosition) {
        if (getLayout() != null) {
            return getLayout().getLineForOffset(characterPosition);
        }
        return -1;
    }

    public void setCurrentLineNumber(int currentLineNumber) {
        this.currentLineNumber = currentLineNumber;
    }

    public void setListener(PageBoundsReachedListener listener) {
        this.listener = listener;
    }

    public PageBoundsReachedListener getListener() {
        return listener;
    }

    /**
     * Checks to see if there is more text available and it's not just a bunch
     * of empty lines.
     * @return true if more text is available.
     */
    public boolean hasMoreText() {
        for(int i = currentLineNumber; i < getLineCount() - 1; i++) {
            String text = getTextAtLine(i);
            if(!text.equals("\n" ) && !text.isEmpty() && text != null) {
                return true;
            }
        }
        return false;
    }

    /**
     * Gets the last line with text on.
     * @return index of the last valid line.
     */
    private int findLastNonEmptyLine() {
        int lineNumber = getLineCount() - 1;
        for(int i = lineNumber; i > 0; i--) {
            String text = getTextAtLine(i);
            if(text != "\n" && !text.isEmpty() && text != null) {
                return i;
            }
        }
        return 0;
    }

    private void updateNumberOfWordsPerLine(int n) {
        String text = getText().toString();
        String[] split = text.split("\\s+");
        StringBuffer buffer = new StringBuffer();
        for(int i = 0; i < split.length; i++) {
            if(i % n == 0) {
                buffer.append(split[i].concat("\n"));
            } else {
                buffer.append(split[i]);
            }
        }
        setText(buffer.toString());
    }

    public void updateView() {
        highlightLine(currentLineNumber);
        updateNumberOfWordsPerLine(wordCountPerLine);
    }

    private static class FixationBoxColorSpan implements LineBackgroundSpan {
        private final int color;

        public FixationBoxColorSpan(int color) {
            this.color = color;
        }

        @Override
        public void drawBackground(Canvas c, Paint p, int left, int right, int top, int baseline,
                                   int bottom, CharSequence text, int start, int end, int lnum) {
            final int paintColor = p.getColor();
            p.setColor(color);
            c.drawRect(new Rect(left, top, right, bottom), p);
            p.setColor(paintColor);
        }
    }
}