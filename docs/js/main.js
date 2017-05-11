$.fn.visibleHeight = function() {
    var elBottom, elTop, scrollBot, scrollTop, visibleBottom, visibleTop;
    scrollTop = $(window).scrollTop();
    scrollBot = scrollTop + $(window).height();
    elTop = this.offset().top;
    elBottom = elTop + this.outerHeight();
    visibleTop = elTop < scrollTop ? scrollTop : elTop;
    visibleBottom = elBottom > scrollBot ? scrollBot : elBottom;
    return visibleBottom - visibleTop
}

function setNavigationHeight() {
	var windowHeight = $(window).height();
	var navHeight = $('.navbar.navbar-default').height();
	var visibleFooterHeight = Math.max(0, $("body > footer").visibleHeight());
	var navigationHeight = windowHeight - navHeight - visibleFooterHeight;
	var visibleToggleNavigationHeight = $(window).width() <= 1024 ? $('#toggle-navigation').outerHeight() : 0;
	$('#navigation .tab-content').height(navigationHeight - $('#navigation .nav-tabs').height() - visibleToggleNavigationHeight);
}

function setNavbarHeight() {
	if($('#navbar-menu').hasClass('in')) {
		var windowHeight = $(window).height();
		var navHeight = $('.navbar.navbar-default').height();
		$('#navbar-menu').css('cssText', 'height:' + (windowHeight - navHeight) + 'px !important');
	}
}

function setMinContainerHeight() {
	var windowHeight = $(window).height();
	var navHeight = $('.navbar.navbar-default').outerHeight();
	var footerHeight = $("body > footer").outerHeight();
	$('#content').css('min-height', windowHeight - navHeight - footerHeight);
}

$(document).ready(function() {
	setNavigationHeight();
	setMinContainerHeight();

	$('.to-top-small-logo-container').click(function (e) {
	    e.preventDefault();
	    $("html, body").animate({scrollTop: 0}, 400, "swing");
	    return false;
	});

	$('#navbar-menu').on('shown.bs.collapse', function (e) {
		setNavbarHeight();
	});

	$('#navbar-menu').on('hidden.bs.collapse', function (e) {
		$(this).css('cssText', 'height: 0 !important');
	});

	$('#toggle-navigation').click(function() {
		$('#navigation, #toggle-navigation').toggleClass('collapsed');
	});
});

$(window).resize(function() {
	setNavigationHeight();
	setNavbarHeight();
	setMinContainerHeight();
});

$(window).scroll(function() {
	setNavigationHeight();
});