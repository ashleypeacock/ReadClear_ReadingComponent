package neurodiversity.readclearcomponent;

/**
 * Signals whether we have reached the top or bottom of a page.
 **/

public interface PageBoundsReachedListener {

    /**
     * Triggerd when the view is at the bottom of the page and there is an attempt to go to the next line.
     */
    void endOfPage();

    /**
     * Triggerd when the view is at the top of the page and there is an attempt to go to the next line.
     */
    void startOfPage();

}
