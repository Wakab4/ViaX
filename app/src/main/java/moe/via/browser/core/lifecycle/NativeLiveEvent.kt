package moe.via.browser.core.lifecycle

import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import java.util.concurrent.atomic.AtomicBoolean
import moe.via.browser.core.logging.NativeLog

/** k8.n delivers each pending event to one active observer; postValue uses the same setValue path. */
internal class NativeLiveEvent<T> : MutableLiveData<T>() {
    private val pending = AtomicBoolean(false)
    override fun observe(owner: LifecycleOwner, observer: Observer<in T>) {
        if (hasActiveObservers()) NativeLog.warn(null, "Multiple observers registered but only one will be notified of changes.")
        super.observe(owner) { value -> if (pending.compareAndSet(true, false)) observer.onChanged(value) }
    }
    override fun setValue(value: T?) { pending.set(true); super.setValue(value) }
}
